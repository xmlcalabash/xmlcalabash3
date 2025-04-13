package com.xmlcalabash.steps.archives

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.s9api.QName
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.attribute.FileTime
import kotlin.io.path.inputStream

open class ArInputArchive(stepConfig: StepConfiguration, doc: XProcBinaryDocument): InputArchive(stepConfig, doc) {
    override val archiveFormat = Ns.ar
    override val baseUri = doc.baseURI
    lateinit var archive: ArArchiveInputStream

    protected fun openAr() {
        openArchive(".a")
        archive = ArArchiveInputStream(BufferedInputStream(archiveFile.inputStream()))
        var index = 0
        for (entry in archive) {
            if (entry.isDirectory) {
                continue
            }

            val archiveEntry = XArchiveEntry(stepConfig, entry.name, entry, this)
            val amap = mutableMapOf<QName, String>(
                Ns.name to entry.name,
                Ns.contentType to "${MediaType.parse(stepConfig.environment.mimeTypes.getContentType(entry.name))}",
                NsCx.size to entry.size.toString(),
                NsCx.groupId to entry.groupId.toString(),
            )
            if (entry.mode != 0) {
                amap[NsCx.mode] = entry.mode.toString()
                amap[NsCx.modeString] = unixModeString(entry.mode)
            }
            if (entry.lastModifiedDate != null) {
                amap[NsCx.lastModified] = iso8601(FileTime.from(entry.lastModifiedDate.toInstant()))
            }

            archiveEntry.properties.putAll(amap)
            archiveEntry.position = index++
            _entries.add(archiveEntry)
        }
        archive.close()
    }

    override fun open() {
        try {
            openAr()
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(Ns.ar), ex)
        }
    }

    override fun close() {
        _entries.clear()
    }

    override fun inputStream(entry: XArchiveEntry): InputStream {
        // I can't see any other way to do this...I could be clever about keeping the
        // stream open and all, but for ar archives, it doesn't seem worth it
        openArchive(".ar")
        archive = ArArchiveInputStream(BufferedInputStream(archiveFile.inputStream()))

        for (arEntry in archive) {
            if (arEntry.isDirectory) {
                continue
            }
            if (arEntry.name == entry.name) {
                val stream = ByteArrayInputStream(archive.readNBytes(arEntry.size.toInt()))
                archive.close()
                return stream
            }
        }

        archive.close()
        throw IllegalStateException("${entry.name} not found")
    }
}