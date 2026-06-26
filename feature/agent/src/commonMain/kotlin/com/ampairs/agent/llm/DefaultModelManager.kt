package com.ampairs.agent.llm

import com.ampairs.auth.api.TokenRepository
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
import io.ktor.client.statement.bodyAsText
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
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
 * The download client is built from the app's platform [HttpClientEngine] with no blanket request
 * timeout (multi-GB files stream for a long time). Bytes come through the backend download proxy
 * (`/api/agent/v1/models/{id}/download`), which is JWT-authenticated, so each request carries the
 * current access token + `X-Workspace-ID`. A per-download Bearer token may override the access token.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultModelManager(
    engine: HttpClientEngine,
    private val storage: ModelStorage,
    private val tokenRepository: TokenRepository,
    /**
     * Platform-contributed archive extractors (zip, …) — empty on platforms with no directory models.
     * Enables the generic "download archive → install as directory" path (`ModelDescriptor.archiveFormat`).
     */
    private val archiveExtractors: List<ArchiveExtractor> = emptyList(),
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
        val path = filePath(model)
        val meta = SystemFileSystem.metadataOrNull(path) ?: return null
        // Both file and directory installs only reach their final path after finalizeDownload
        // validated/extracted them, so presence ⇒ installed. Archive models install as a directory.
        return when {
            model.archiveFormat != null -> if (meta.isDirectory) path.toString() else null
            (meta.size ?: 0L) > 0L -> path.toString()
            else -> null
        }
    }

    override suspend fun refresh() = withContext(DispatcherProvider.io) {
        val updated = _statuses.value.toMutableMap()
        for (model in ModelCatalog.all) {
            if (updated[model.id] is ModelInstallStatus.Downloading) continue // don't clobber live progress
            val size = sizeOf(filePath(model))
            updated[model.id] = if (size != null && size > 0L) {
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
                set(model.id, ModelInstallStatus.Downloading(sizeOf(partPath(model)) ?: 0L, model.sizeBytes))
                SystemFileSystem.createDirectories(dir())
                downloadWithResume(model, authToken)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                set(model.id, ModelInstallStatus.Failed(e.message ?: "Download failed"))
            } finally {
                jobs.remove(model.id)
            }
        }
    }

    /**
     * Run [runDownload], transparently resuming from the `.part` file (HTTP `Range`) when the
     * connection drops mid-stream — common for these multi-GB models on mobile networks. The retry
     * budget resets whenever bytes advance, so a download over a flaky link keeps going as long as it
     * makes progress and only gives up after [MAX_STALLED_RETRIES] consecutive attempts with no new
     * bytes. Terminal failures (size/checksum mismatch) don't throw here — [runDownload] sets
     * [ModelInstallStatus.Failed] and returns, so they aren't retried.
     */
    private suspend fun downloadWithResume(model: ModelDescriptor, authToken: String?) {
        var stalledRetries = 0
        var lastBytes = sizeOf(partPath(model)) ?: 0L
        while (true) {
            try {
                runDownload(model, authToken)
                return
            } catch (c: CancellationException) {
                throw c
            } catch (e: TerminalDownloadException) {
                // Auth/gated/not-found won't change on retry — surface immediately, no resume loop.
                // (This is what prevented the gated-model 502 from being hammered 5× with backoff.)
                throw e
            } catch (e: Exception) {
                val bytes = sizeOf(partPath(model)) ?: 0L
                if (bytes > lastBytes) {
                    stalledRetries = 0
                    lastBytes = bytes
                } else {
                    stalledRetries++
                }
                if (stalledRetries >= MAX_STALLED_RETRIES) throw e
                // Keep the UI in "downloading" with the partial progress while we back off, then resume.
                set(model.id, ModelInstallStatus.Downloading(bytes, model.sizeBytes))
                delay(RESUME_BACKOFF_MILLIS * stalledRetries)
            }
        }
    }

    private suspend fun runDownload(model: ModelDescriptor, authToken: String?) {
        val part = partPath(model)
        val alreadyHave = sizeOf(part) ?: 0L
        val wantResume = alreadyHave > 0L

        // The proxy's Content-Length is the authoritative full file size; the catalog sizeBytes is
        // only an estimate (per agent/CLAUDE.md), so we validate against this, not the estimate.
        var expectedTotal: Long? = null
        client.prepareGet(model.downloadUrl) {
            val bearer = authToken ?: tokenRepository.getAccessToken()
            bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            tokenRepository.getWorkspaceIdSync().takeIf { it.isNotBlank() }
                ?.let { header("X-Workspace-ID", it) }
            if (wantResume) header(HttpHeaders.Range, "bytes=$alreadyHave-")
        }.execute { response ->
            // Reject error responses (e.g. proxy 401 gated / 404 / 5xx) instead of writing the error
            // body into the .part file and later failing with a confusing size mismatch. Auth/gated/
            // not-found are terminal (retrying can't help — see TerminalDownloadException); only
            // genuinely transient statuses fall through to downloadWithResume's retry loop.
            if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.PartialContent) {
                val code = response.status.value
                val body = runCatching { response.bodyAsText() }.getOrNull()
                if (code in TERMINAL_STATUSES) throw TerminalDownloadException(terminalMessage(code, body))
                throw IOException("Download failed: server returned $code")
            }
            val resumed = wantResume && response.status == HttpStatusCode.PartialContent
            if (!resumed) SystemFileSystem.delete(part, mustExist = false)

            var downloaded = if (resumed) alreadyHave else 0L
            // On a 206 the Content-Length is the remaining bytes, so add what we already have.
            expectedTotal = response.contentLength()?.let { if (resumed) it + alreadyHave else it }
            val total = expectedTotal ?: model.sizeBytes.takeIf { it > 0 } ?: -1L
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

        finalizeDownload(model, part, expectedTotal)
    }

    private suspend fun finalizeDownload(model: ModelDescriptor, part: Path, expectedTotal: Long?) {
        val size = sizeOf(part) ?: 0L
        if (size <= 0L) {
            SystemFileSystem.delete(part, mustExist = false)
            set(model.id, ModelInstallStatus.Failed("Download produced no data"))
            return
        }
        // Validate against the server's authoritative Content-Length when known; never hard-fail on
        // the catalog sizeBytes estimate (which can legitimately differ from the real file).
        if (expectedTotal != null && expectedTotal > 0 && size != expectedTotal) {
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
        val format = model.archiveFormat
        if (format != null) {
            finalizeArchive(model, part, format, size)
            return
        }
        val dest = filePath(model)
        SystemFileSystem.delete(dest, mustExist = false)
        SystemFileSystem.atomicMove(part, dest)
        set(model.id, ModelInstallStatus.Installed(size))
    }

    /**
     * Install an archive model: extract the validated `.part` into a temp dir via the matching
     * [ArchiveExtractor], unwrap a single top-level folder if present (Vosk-style archives nest one),
     * then atomically swap it into [filePath]. The archive + temp dir are removed on success or failure.
     */
    private suspend fun finalizeArchive(model: ModelDescriptor, part: Path, format: ArchiveFormat, archiveSize: Long) {
        val extractor = archiveExtractors.firstOrNull { it.supports(format) }
        if (extractor == null) {
            SystemFileSystem.delete(part, mustExist = false)
            set(model.id, ModelInstallStatus.Failed("No $format extractor available on this platform"))
            return
        }
        // Keep the UI at ~100% while we unpack — extraction is the brief tail of the install.
        set(model.id, ModelInstallStatus.Downloading(archiveSize, archiveSize))
        val dest = filePath(model)
        val tmp = Path(dir(), model.fileName + EXTRACT_SUFFIX)
        deleteRecursively(tmp)
        try {
            extractor.extract(part.toString(), tmp.toString(), format)
            val root = singleChildDirOrSelf(tmp)
            deleteRecursively(dest)
            SystemFileSystem.atomicMove(root, dest)
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            deleteRecursively(tmp)
            deleteRecursively(dest)
            SystemFileSystem.delete(part, mustExist = false)
            set(model.id, ModelInstallStatus.Failed(e.message ?: "Extraction failed"))
            return
        }
        deleteRecursively(tmp)
        SystemFileSystem.delete(part, mustExist = false)
        set(model.id, ModelInstallStatus.Installed(archiveSize))
    }

    /** When [dir] holds exactly one entry and it's a directory, return it (the archive's wrapper folder). */
    private fun singleChildDirOrSelf(dir: Path): Path {
        val children = runCatching { SystemFileSystem.list(dir) }.getOrDefault(emptyList())
        val only = children.singleOrNull() ?: return dir
        return if (SystemFileSystem.metadataOrNull(only)?.isDirectory == true) only else dir
    }

    /** Recursively delete a file or directory tree (kotlinx-io's delete only handles empty dirs). */
    private fun deleteRecursively(path: Path) {
        val meta = SystemFileSystem.metadataOrNull(path) ?: return
        if (meta.isDirectory) {
            for (child in SystemFileSystem.list(path)) deleteRecursively(child)
        }
        SystemFileSystem.delete(path, mustExist = false)
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
            deleteRecursively(filePath(model)) // handles both file and directory (archive) installs
            SystemFileSystem.delete(partPath(model), mustExist = false)
            deleteRecursively(Path(dir(), model.fileName + EXTRACT_SUFFIX)) // stray temp, if any
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

    /** A download failure that won't change on retry (auth/gated/not-found) — fail fast, no resume loop. */
    private class TerminalDownloadException(message: String) : Exception(message)

    /** User-facing message for a terminal HTTP status; the proxy maps a gated upstream repo to 502. */
    private fun terminalMessage(code: Int, body: String?): String = when {
        code == 401 || code == 403 || (code == 502 && body?.contains("authentication", ignoreCase = true) == true) ->
            "This AI model is license-gated. The server needs a HuggingFace token (AGENT_HF_TOKEN) that has " +
                "accepted the model license. Ask your admin to configure it, then try again."
        code == 404 -> "Model file not found on the server."
        else -> "Download failed (server returned $code)."
    }

    private companion object {
        const val PART_SUFFIX = ".part"
        const val EXTRACT_SUFFIX = ".extract" // temp dir an archive is unpacked into before the atomic swap
        /** HTTP statuses that won't resolve on retry — surfaced immediately instead of resumed. */
        val TERMINAL_STATUSES = setOf(401, 403, 404, 502)
        const val DOWNLOAD_CHUNK = 64L * 1024
        const val PROGRESS_STEP = 1_000_000L // emit progress roughly per ~1 MB
        const val HEX = "0123456789abcdef"
        const val MAX_STALLED_RETRIES = 5 // consecutive no-progress resume attempts before giving up
        const val RESUME_BACKOFF_MILLIS = 2_000L // backoff grows: 2s, 4s, 6s, … between resume attempts
    }
}
