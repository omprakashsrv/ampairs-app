package com.ampairs.agent.llm

import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.kotlincrypto.hash.sha2.SHA256

/**
 * kodio-free, Ktor-based on-device model downloader (T032). Streams to a `.part` file (resumable via
 * HTTP `Range`), verifies size (+ optional SHA-256), then atomically moves it into place. Install
 * state is derived from the filesystem — see [ModelManager].
 *
 * The download client is built from the app's platform [HttpClientEngine] but is deliberately
 * **un-authenticated** (no JWT/workspace interceptors): model files come from a CDN/HF, not our API.
 * A per-download Bearer token can be supplied for gated/self-hosted URLs.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultModelManager(
    engine: HttpClientEngine,
    private val storage: ModelStorage,
) : ModelManager {

    private val scope = CoroutineScope(SupervisorJob() + DispatcherProvider.io)
    private val jobs = mutableMapOf<String, Job>()

    private val client = HttpClient(engine) {
        followRedirects = true // HF/CDN endpoints commonly 302 to a signed file URL
        install(HttpTimeout) {
            // No requestTimeout (large files stream for a long time); guard the socket only.
            socketTimeoutMillis = 60_000
        }
    }

    private val _statuses = MutableStateFlow<Map<String, ModelInstallStatus>>(emptyMap())
    override val statuses: StateFlow<Map<String, ModelInstallStatus>> = _statuses.asStateFlow()

    override fun statusOf(modelId: String): ModelInstallStatus =
        _statuses.value[modelId] ?: ModelInstallStatus.NotInstalled

    private fun set(modelId: String, status: ModelInstallStatus) =
        _statuses.update { it + (modelId to status) }

    private fun dir(): Path = Path(storage.modelsDirectoryPath())
    private fun filePath(model: ModelDescriptor): Path = Path(dir(), model.fileName)
    private fun partPath(model: ModelDescriptor): Path = Path(dir(), model.fileName + PART_SUFFIX)

    private fun sizeOf(path: Path): Long? = SystemFileSystem.metadataOrNull(path)?.size

    override fun localPathOrNull(model: ModelDescriptor): String? {
        val size = sizeOf(filePath(model)) ?: return null
        return if (model.sizeBytes <= 0 || size == model.sizeBytes) filePath(model).toString() else null
    }

    override suspend fun refresh() = withContext(DispatcherProvider.io) {
        val updated = _statuses.value.toMutableMap()
        for (model in ModelCatalog.all) {
            if (updated[model.id] is ModelInstallStatus.Downloading) continue // don't clobber live progress
            val size = sizeOf(filePath(model))
            updated[model.id] = if (size != null && (model.sizeBytes <= 0 || size == model.sizeBytes)) {
                ModelInstallStatus.Installed(size)
            } else {
                ModelInstallStatus.NotInstalled
            }
        }
        _statuses.value = updated
    }

    override fun download(model: ModelDescriptor, authToken: String?) {
        if (statusOf(model.id) is ModelInstallStatus.Downloading) return
        jobs[model.id]?.cancel()
        jobs[model.id] = scope.launch {
            try {
                set(model.id, ModelInstallStatus.Downloading(0, model.sizeBytes))
                SystemFileSystem.createDirectories(dir())
                runDownload(model, authToken)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                set(model.id, ModelInstallStatus.Failed(e.message ?: "Download failed"))
            } finally {
                jobs.remove(model.id)
            }
        }
    }

    private suspend fun runDownload(model: ModelDescriptor, authToken: String?) {
        val part = partPath(model)
        val alreadyHave = sizeOf(part) ?: 0L
        val wantResume = alreadyHave in 1 until (if (model.sizeBytes > 0) model.sizeBytes else Long.MAX_VALUE)

        client.prepareGet(model.downloadUrl) {
            authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            if (wantResume) header(HttpHeaders.Range, "bytes=$alreadyHave-")
        }.execute { response ->
            val resumed = wantResume && response.status == HttpStatusCode.PartialContent
            if (!resumed) SystemFileSystem.delete(part, mustExist = false)

            var downloaded = if (resumed) alreadyHave else 0L
            val total = when {
                model.sizeBytes > 0 -> model.sizeBytes
                else -> response.contentLength()?.let { it + downloaded } ?: -1L
            }
            var lastEmitted = downloaded

            val channel: ByteReadChannel = response.bodyAsChannel()
            SystemFileSystem.sink(part, append = resumed).buffered().use { sink ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DOWNLOAD_CHUNK)
                    while (!packet.exhausted()) {
                        val bytes = packet.readByteArray()
                        sink.write(bytes)
                        downloaded += bytes.size
                        if (downloaded - lastEmitted >= PROGRESS_STEP) {
                            lastEmitted = downloaded
                            set(model.id, ModelInstallStatus.Downloading(downloaded, total))
                        }
                    }
                }
                sink.flush()
            }
        }

        finalizeDownload(model, part)
    }

    private fun finalizeDownload(model: ModelDescriptor, part: Path) {
        val size = sizeOf(part) ?: 0L
        if (model.sizeBytes > 0 && size != model.sizeBytes) {
            SystemFileSystem.delete(part, mustExist = false)
            set(model.id, ModelInstallStatus.Failed("Downloaded size does not match expected size"))
            return
        }
        model.sha256?.let { expected ->
            val actual = sha256Hex(part)
            if (!actual.equals(expected, ignoreCase = true)) {
                SystemFileSystem.delete(part, mustExist = false)
                set(model.id, ModelInstallStatus.Failed("Checksum mismatch"))
                return
            }
        }
        val dest = filePath(model)
        SystemFileSystem.delete(dest, mustExist = false)
        SystemFileSystem.atomicMove(part, dest)
        set(model.id, ModelInstallStatus.Installed(size))
    }

    override fun cancel(modelId: String) {
        jobs.remove(modelId)?.cancel()
        // Leave the .part file for a later resume; reflect that nothing is installed yet.
        if (statusOf(modelId) is ModelInstallStatus.Downloading) {
            set(modelId, ModelInstallStatus.NotInstalled)
        }
    }

    override suspend fun delete(model: ModelDescriptor) {
        jobs.remove(model.id)?.cancel()
        withContext(DispatcherProvider.io) {
            SystemFileSystem.delete(filePath(model), mustExist = false)
            SystemFileSystem.delete(partPath(model), mustExist = false)
        }
        set(model.id, ModelInstallStatus.NotInstalled)
    }

    private fun sha256Hex(path: Path): String {
        val digest = SHA256()
        SystemFileSystem.source(path).buffered().use { source ->
            val buf = ByteArray(DOWNLOAD_CHUNK.toInt())
            while (true) {
                val read = source.readAtMostTo(buf, 0, buf.size)
                if (read == -1) break
                if (read > 0) digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") {
            val v = it.toInt() and 0xFF
            HEX[v ushr 4].toString() + HEX[v and 0x0F]
        }
    }

    private companion object {
        const val PART_SUFFIX = ".part"
        const val DOWNLOAD_CHUNK = 64L * 1024
        const val PROGRESS_STEP = 1_000_000L // emit progress roughly per ~1 MB
        const val HEX = "0123456789abcdef"
    }
}
