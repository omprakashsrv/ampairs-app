package com.ampairs.printing.service

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.engine.PrintEngine
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintJobState
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateKind
import com.ampairs.printing.core.model.ValueFormatter
import com.ampairs.printing.core.provider.PrintValueProvider
import com.ampairs.file.api.FileRepository
import com.ampairs.printing.core.spool.PrintJob
import com.ampairs.printing.core.spool.SendOutcome
import com.ampairs.printing.core.spool.SpoolPolicy
import com.ampairs.printing.data.repository.PrintJobRepository
import com.ampairs.printing.data.repository.PrinterRepository
import com.ampairs.printing.data.repository.TemplateRepository
import com.ampairs.printing.template.DefaultTemplates
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock

/**
 * End-to-end print entry point: resolve the routed printer + the active template (falling back to a
 * seeded default), build the [com.ampairs.printing.core.model.PrintDocument] via [PrintEngine], record
 * a spooled [PrintJob], and send via [PrintService]. The job state follows [SpoolPolicy] (§19).
 *
 * The caller (ViewModel) supplies the [PrintValueProvider] for the document type and an
 * [idempotencyKey] so a retry can never double-print.
 */
@Inject
@SingleIn(WorkspaceScope::class)
class PrintCoordinator(
    private val printerRepository: PrinterRepository,
    private val templateRepository: TemplateRepository,
    private val jobRepository: PrintJobRepository,
    private val printService: PrintService,
    private val fileRepository: FileRepository,
    /** All document providers, keyed by type — lets [retry] rebuild any spooled job. */
    private val providers: Map<DocumentType, PrintValueProvider>,
) {
    private val engine = PrintEngine()
    private val log = Logger.withTag("PrintCoordinator")

    /**
     * Print a document once. If a job already exists for [idempotencyKey] and is in-flight or done,
     * this is a no-op that reports the existing outcome — a retry can never double-print (§19).
     */
    suspend fun print(
        provider: PrintValueProvider,
        documentId: String,
        idempotencyKey: String,
        copies: Int = 1,
        formatter: ValueFormatter = PlainValueFormatter,
    ): Result<SendOutcome> {
        jobRepository.get(idempotencyKey)?.let { existing ->
            if (SpoolPolicy.blocksReprint(existing.state)) {
                return Result.success(SpoolPolicy.outcomeFor(existing.state))
            }
        }
        return send(provider, documentId, idempotencyKey, copies, formatter, printerId = null)
    }

    /** Manually retry a FAILED/DEAD/UNCONFIRMED job — rebuilds the document on its original printer. */
    suspend fun retry(jobId: String, formatter: ValueFormatter = PlainValueFormatter): Result<SendOutcome> {
        val job = jobRepository.get(jobId)
            ?: return Result.failure(IllegalStateException("Print job not found: $jobId"))
        val documentType = DocumentType.entries.firstOrNull { it.key == job.documentType }
            ?: return Result.failure(IllegalStateException("Unknown document type ${job.documentType}"))
        val provider = providers[documentType]
            ?: return Result.failure(IllegalStateException("No provider for ${documentType.key}"))
        return send(provider, job.documentId, job.idempotencyKey, job.copies, formatter, printerId = job.printerId)
    }

    /** Whether a printer is routed for [documentType] (so the new-module print path can be used). */
    suspend fun hasRoutedPrinter(documentType: DocumentType): Boolean =
        printerRepository.resolvePrinter(documentType.key) != null

    /** Whether ANY printer is configured at all — if false, the UI should launch printer setup. */
    suspend fun hasAnyPrinter(): Boolean =
        printerRepository.resolvePrinter(DocumentType.INVOICE.key) != null || printerRepository.anyPrinter() != null

    /** User confirms a SENT_UNCONFIRMED job actually printed — moves it to CONFIRMED. */
    suspend fun markPrinted(jobId: String) {
        val job = jobRepository.get(jobId) ?: return
        jobRepository.save(
            job.copy(state = PrintJobState.CONFIRMED, updatedAtMillis = Clock.System.now().toEpochMilliseconds()),
        )
    }

    private suspend fun send(
        provider: PrintValueProvider,
        documentId: String,
        idempotencyKey: String,
        copies: Int,
        formatter: ValueFormatter,
        printerId: String?,
    ): Result<SendOutcome> {
        val documentType = provider.documentType
        log.i { "send: docType=${documentType.key} id=$documentId key=$idempotencyKey printerIdOverride=$printerId" }
        // Prefer the explicit printer (retry), then the routed printer, then ANY configured printer.
        val profile = (if (printerId != null) printerRepository.getProfile(printerId) else null)
            ?: printerRepository.resolvePrinter(documentType.key)
            ?: printerRepository.anyPrinter()
            ?: run {
                log.w { "send: no printer configured for ${documentType.key}" }
                return Result.failure(IllegalStateException("No printer configured"))
            }
        log.i { "send: resolved printer name=${profile.name} class=${profile.printerClass} conn=${profile.connectionType} addr=${profile.address}" }

        // Ensure the built-in templates exist locally (and get pushed to the backend) on first use.
        templateRepository.seedDefaultsIfEmpty()
        // Pick the template matching the printer's class (thermal vs page) for this document type.
        val template: Template = templateRepository.firstTemplate(documentType.key, profile.printerClass)
            ?: defaultTemplate(documentType, profile.printerClass)
            ?: return Result.failure(IllegalStateException("No template for ${documentType.key}"))
        log.i { "send: using template=${template.id} (class=${template.printerClass})" }

        val existing = jobRepository.get(idempotencyKey)
        val now = Clock.System.now().toEpochMilliseconds()
        var job = PrintJob(
            id = idempotencyKey,
            idempotencyKey = idempotencyKey,
            documentType = documentType.key,
            documentId = documentId,
            templateId = template.id,
            printerId = profile.id,
            copies = copies,
            state = PrintJobState.SENDING,
            attempts = existing?.attempts ?: 0,
            createdAtMillis = existing?.createdAtMillis ?: now,
            updatedAtMillis = now,
        )
        jobRepository.save(job)

        return runCatching {
            val document = if (template.kind == TemplateKind.STATIC && template.htmlFileUid != null) {
                val html = fileRepository.readFileBytes(template.htmlFileUid!!).getOrThrow().decodeToString()
                engine.buildStatic(template, documentId, provider, html)
            } else {
                engine.build(template, documentId, provider)
            }
            log.i { "send: built document blocks=${document.blocks.size}; calling PrintService" }
            val outcome = printService.print(document, profile, formatter)
            log.i { "send: PrintService outcome=$outcome" }
            job = job.copy(
                state = SpoolPolicy.afterSend(job, outcome),
                attempts = job.attempts + 1,
                updatedAtMillis = Clock.System.now().toEpochMilliseconds(),
            )
            jobRepository.save(job)
            outcome
        }.onFailure { error ->
            log.e(error) { "send: print FAILED for ${documentType.key} on ${profile.name}: ${error.message}" }
            jobRepository.save(
                job.copy(
                    state = PrintJobState.FAILED,
                    attempts = job.attempts + 1,
                    lastError = error.message,
                    updatedAtMillis = Clock.System.now().toEpochMilliseconds(),
                ),
            )
        }
    }

    /** Seeded fallback template, matched to the target printer's class (page printers get A4). */
    private fun defaultTemplate(documentType: DocumentType, printerClass: PrinterClass): Template? = when (documentType) {
        DocumentType.INVOICE ->
            if (printerClass == PrinterClass.PAGE) DefaultTemplates.pageInvoice()
            else DefaultTemplates.thermalInvoice(PaperSpec.THERMAL_80)
        DocumentType.ORDER -> DefaultTemplates.thermalOrder(PaperSpec.THERMAL_80)
        DocumentType.RECEIPT -> DefaultTemplates.thermalReceipt(PaperSpec.THERMAL_58)
        else -> null
    }
}
