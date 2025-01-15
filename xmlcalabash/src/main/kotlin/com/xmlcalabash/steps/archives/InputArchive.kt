package com.xmlcalabash.steps.archives

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.FileUtils
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.format.DateTimeFormatter

abstract class InputArchive(stepConfig: XProcStepConfiguration, val doc: XProcBinaryDocument): AbstractArchiveFile(stepConfig) {
    protected val _entries = mutableListOf< XArchiveEntry>()
    val entries: List<XArchiveEntry>
        get() = _entries

    internal fun openArchive(ext: String) {
        archiveFile = Files.createTempFile(tdir, null, ext)
        archiveFile.toFile().deleteOnExit()
        temporary = true
        val stream = FileUtils.outputStream(archiveFile)
        stream.write(doc.binaryValue)
        stream.close()
    }

    abstract val baseUri: URI?
    abstract fun open()
    abstract fun close()
    abstract fun inputStream(entry: XArchiveEntry): InputStream

    protected fun iso8601(time: FileTime): String {
        val instant = Instant.ofEpochMilli(time.toMillis())
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }

    protected fun unixModeString(bits: Int): String {
        val chars = arrayOf('r', 'w', 'x')
        var pos = 0
        var mask = 0x1 shl 8
        val sb = StringBuilder()
        while (mask > 0) {
            if (bits and mask == mask) {
                sb.append(chars[pos])
            } else {
                sb.append('-')
            }
            pos = if (pos == 2) { 0 } else { pos + 1 }
            mask = mask shr 1
        }
        return sb.toString()
    }
}