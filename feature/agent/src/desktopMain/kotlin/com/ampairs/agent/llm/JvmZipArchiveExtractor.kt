package com.ampairs.agent.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * JVM [ArchiveExtractor] for [ArchiveFormat.ZIP] (Desktop) using `java.util.zip` — no extra dependency.
 * Guards against zip-slip (entries escaping [destDir] via `..`) and runs on [Dispatchers.IO].
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
                    // Zip-slip guard: the resolved path must stay inside destDir.
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
