package com.ampairs.printing.service

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.engine.PrintEngine
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintJobState
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.ValueFormatter
import com.ampairs.printing.core.provider.PrintValueProvider
import com.ampairs.printing.core.spool.PrintJob
import com.ampairs.printing.core.spool.SendOutcome
import com.ampairs.printing.core.spool.SpoolPolicy
import com.ampairs.printing.data.repository.PrintJobRepository
import com.ampairs.printing.data.repository.PrinterRepository
import com.ampairs.printing.data.repository.TemplateRepository
import com.ampairs.printing.template.DefaultTemplates
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
    /** All document providers, keyed by type — lets [retry] rebuild any spooled job. */
    private val providers: Map<DocumentType, PrintValueProvider>,
) {
    private val engine = PrintEngine()

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
        val profile = (if (printerId != null) printerRepository.getProfile(printerId) else null)
            ?: printerRepository.resolvePrinter(documentType.key)
            ?: return Result.failure(IllegalStateException("No printer routed for ${documentType.key}"))

        val template: Template = templateRepository.firstTemplate(documentType.key)
            ?: defaultTemplate(documentType)
            ?: return Result.failure(IllegalStateException("No template for ${documentType.key}"))

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
            val document = engine.build(template, documentId, provider)
            val outcome = printService.print(document, profile, formatter)
            job = job.copy(
                state = SpoolPolicy.afterSend(job, outcome),
                attempts = job.attempts + 1,
                updatedAtMillis = Clock.System.now().toEpochMilliseconds(),
            )
            jobRepository.save(job)
            outcome
        }.onFailure { error ->
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

    private fun defaultTemplate(documentType: DocumentType): Template? = when (documentType) {
        DocumentType.INVOICE -> DefaultTemplates.thermalInvoice(PaperSpec.THERMAL_80)
        DocumentType.RECEIPT -> DefaultTemplates.thermalReceipt(PaperSpec.THERMAL_58)
        else -> null
    }
}
