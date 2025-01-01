package com.xmlcalabash.steps.archives

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import java.io.InputStream
import java.nio.file.Files
import kotlin.String

class SevenZInputArchive(stepConfig: XProcStepConfiguration, doc: XProcBinaryDocument): InputArchive(stepConfig, doc) {
    override val archiveFormat = Ns.sevenZ
    override val baseUri = doc.baseURI
    lateinit var archive: SevenZFile

    protected fun open7z() {
        openArchive(".7z")
        val zbuilder = SevenZFile.Builder()
        zbuilder.setSeekableByteChannel(Files.newByteChannel(archiveFile))
        archive = zbuilder.get()
        var index = 0
        for (entry in archive.entries) {
            if (entry.isDirectory) {
                continue
            }
            val archiveEntry = XArchiveEntry(stepConfig, entry.name, entry, this)
            val amap = mutableMapOf<QName, String>(
                Ns.name to entry.name,
                Ns.contentType to "${MediaType.parse(stepConfig.environment.mimeTypes.getContentType(entry.name))}",
                NsCx.size to entry.size.toString()
            )
            if (entry.hasCrc) {
                NsCx.crc to entry.crcValue.toString()
            }
            if (entry.hasCreationDate && entry.creationTime != null) {
                amap[NsCx.fileCreationTime] = iso8601(entry.creationTime)
            }
            if (entry.hasAccessDate && entry.accessTime != null) {
                amap[NsCx.lastAccessTime] = iso8601(entry.accessTime)
            }
            if (entry.hasLastModifiedDate && entry.lastModifiedTime != null) {
                amap[NsCx.lastModified] = iso8601(entry.lastModifiedTime)
            }
            if (entry.hasWindowsAttributes) {
                // https://py7zr.readthedocs.io/en/latest/archive_format.html
                val bits = entry.windowsAttributes
                if (bits and 0x8000 != 0) {
                    val unix = (bits ushr 16) and 0xFFFF;
                    amap[NsCx.mode] = unix.toString()
                    amap[NsCx.modeString] = unixModeString(unix)
                }

                val rbits = bits and 0xFFFF;
                amap[NsCx.windowsAttributes] = rbits.toString()
            }
            if (entry.isAntiItem) {
                amap[NsCx.antiItem] = "true"
            }
            if (entry.contentMethods != null) {
                val sb = StringBuilder()
                var first = true
                for (method in entry.contentMethods) {
                    if (!first) {
                        sb.append(" ")
                    } else {
                        first = false
                    }
                    when (method.method) {
                        SevenZMethod.COPY -> sb.append("none")
                        SevenZMethod.LZMA -> sb.append("lzma")
                        SevenZMethod.LZMA2 -> sb.append("lzma2")
                        SevenZMethod.DEFLATE -> sb.append("deflate")
                        SevenZMethod.DEFLATE64 -> sb.append("deflate64")
                        SevenZMethod.BZIP2 -> sb.append("bzip2")
                        else -> sb.append("unknown")
                    }
                }
                amap[Ns.method] = sb.toString()
            }
            archiveEntry.properties.putAll(amap)
            archiveEntry.position = index++
            _entries.add(archiveEntry)
        }
    }

    override fun open() {
        try {
            open7z()
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(Ns.sevenZ), ex)
        }
    }

    override fun close() {
        archive.close()
        _entries.clear()
    }

    override fun inputStream(entry: XArchiveEntry): InputStream {
        return archive.getInputStream(entry.entry as SevenZArchiveEntry)
    }

}