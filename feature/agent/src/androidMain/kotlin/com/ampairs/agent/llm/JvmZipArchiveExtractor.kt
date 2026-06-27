package com.ampairs.agent.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Android [ArchiveExtractor] for [ArchiveFormat.ZIP] using `java.util.zip` (same as the Desktop one).
 * Enables directory-model installs (Vosk zips) into internal storage. Zip-slip guarded; runs on
 * [Dispatchers.IO].
 */
class JvmZipArchiveExtractor : ArchiveExtractor {

    override fun supports(format: ArchiveFormat): Boolean = format == ArchiveFormat.ZIP

    override suspend fun extract(archivePath: String, destDir: String, format: ArchiveFormat): Unit =
        withContext(Dispatchers.IO) {
            val root = File(destDir)
            root.mkdirs()
            val rootCanonical = root.canonicalFile
            ZipInputStream(File(archivePath).inputStream().buffered()).use { zin ->
                var entry = zin.nextEntry
                while (entry != null) {
                    val target = File(root, entry.name)
                    if (!target.canonicalFile.toPath().startsWith(rootCanonical.toPath())) {
                        throw SecurityException("Zip entry escapes target dir: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { out -> zin.copyTo(out) }
                    }
                    zin.closeEntry()
                    entry = zin.nextEntry
                }
            }
        }
}
