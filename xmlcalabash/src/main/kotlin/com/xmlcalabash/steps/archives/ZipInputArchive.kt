package com.xmlcalabash.steps.archives

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.s9api.QName
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.InputStream
import java.util.zip.ZipEntry

open class ZipInputArchive(stepConfig: StepConfiguration, doc: XProcBinaryDocument): InputArchive(stepConfig, doc) {
    override val archiveFormat = Ns.zip
    override val baseUri = doc.baseURI
    lateinit var archive: ZipFile

    protected fun openZip(ext: String) {
        openArchive(ext)
        val zipBuilder = ZipFile.Builder()
        zipBuilder.setFile(archiveFile.toFile())
        archive = zipBuilder.get()
        var index = 0
        for (entry in archive.entries) {
            if (entry.isDirectory) {
                continue
            }

            val archiveEntry = XArchiveEntry(stepConfig, entry.name, entry, this)
            val amap = mutableMapOf<QName, String>(
                Ns.name to entry.name,
                Ns.contentType to "${MediaType.parse(stepConfig.documentManager.mimetypesFileTypeMap.getContentType(entry.name))}",
                NsCx.size to entry.size.toString()
            )
            if (entry.unixMode != 0) {
                amap[NsCx.mode] = entry.unixMode.toString()
                amap[NsCx.modeString] = unixModeString(entry.unixMode)
            }
            if (entry.creationTime != null) {
                amap[NsCx.fileCreationTime] = iso8601(entry.creationTime)
            }
            if (entry.lastAccessTime != null) {
                amap[NsCx.lastAccessTime] = iso8601(entry.lastAccessTime)
            }
            if (entry.lastModifiedTime != null) {
                amap[NsCx.lastModified] = iso8601(entry.lastModifiedTime)
            }
            if (entry.externalAttributes != 0L) {
                amap[NsCx.externalAttributes] = "${entry.externalAttributes}"
            }
            // generalPurposeBit
            when (entry.method) {
                ZipEntry.STORED -> amap[Ns.method] = "none"
                ZipEntry.DEFLATED -> amap[Ns.method] = "deflated"
                else -> Unit
            }

            if (entry.comment != null && entry.comment.isNotEmpty()) {
                amap[Ns.comment] = entry.comment
            }
            archiveEntry.properties.putAll(amap)
            archiveEntry.position = index++
            _entries.add(archiveEntry)
        }
    }

    override fun open() {
        try {
            openZip(".zip")
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(Ns.zip), ex)
        }
    }

    override fun close() {
        archive.close()
        _entries.clear()
    }

    override fun inputStream(entry: XArchiveEntry): InputStream {
        return archive.getInputStream(entry.entry as ZipArchiveEntry)
    }

}