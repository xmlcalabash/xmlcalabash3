package com.xmlcalabash.steps.archives

import com.xmlcalabash.runtime.XProcStepConfiguration
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant

abstract class OutputArchive(stepConfig: XProcStepConfiguration): AbstractArchiveFile(stepConfig) {
    protected val _entries = mutableListOf< XArchiveEntry>()
    val entries: List<XArchiveEntry>
        get() = _entries

    fun createArchive(ext: String): Path {
        val file = Files.createTempFile(tdir, null, ext)
        file.toFile().deleteOnExit()
        temporary = true
        return file
    }

    val baseUri: URI
        get() = archiveFile.toUri()

    abstract fun create(file: File? = null)
    abstract fun close()

    open fun write(entry: XArchiveEntry) {
        _entries.add(entry)
    }

    protected fun fileTime(isoString: String?): FileTime? {
        if (isoString == null) {
            return null
        }

        val instant = Instant.parse(isoString)
        val time = FileTime.from(Instant.ofEpochMilli(instant.toEpochMilli()))
        return time
    }
}