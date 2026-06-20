package com.ampairs.printing.data.repository

import com.ampairs.printing.core.spool.PrintJob
import com.ampairs.printing.data.db.PrintJobDao
import com.ampairs.printing.data.db.toEntity
import com.ampairs.printing.data.db.toJob
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Device-local print-job spool storage (never synced). */
@Inject
class PrintJobRepository(
    private val jobDao: PrintJobDao,
) {
    fun observeJobs(): Flow<List<PrintJob>> =
        jobDao.observeAll().map { rows -> rows.map { it.toJob() } }

    suspend fun get(id: String): PrintJob? = jobDao.getById(id)?.toJob()

    suspend fun pending(): List<PrintJob> = jobDao.pending().map { it.toJob() }

    suspend fun save(job: PrintJob) = jobDao.upsert(job.toEntity())
}
