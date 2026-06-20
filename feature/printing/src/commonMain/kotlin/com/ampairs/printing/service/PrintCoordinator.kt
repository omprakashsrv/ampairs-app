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
) {
    private val engine = PrintEngine()

    suspend fun print(
        provider: PrintValueProvider,
        documentId: String,
        idempotencyKey: String,
        copies: Int = 1,
        formatter: ValueFormatter = PlainValueFormatter,
    ): Result<SendOutcome> {
        val documentType = provider.documentType
        val profile = printerRepository.resolvePrinter(documentType.key)
            ?: return Result.failure(IllegalStateException("No printer routed for ${documentType.key}"))

        val template: Template = templateRepository.firstTemplate(documentType.key)
            ?: defaultTemplate(documentType)
            ?: return Result.failure(IllegalStateException("No template for ${documentType.key}"))

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
            attempts = 0,
            createdAtMillis = now,
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
