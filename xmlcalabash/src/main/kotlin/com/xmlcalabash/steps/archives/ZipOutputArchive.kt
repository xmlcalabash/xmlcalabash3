package com.xmlcalabash.steps.archives

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.File
import java.nio.file.attribute.FileTime
import java.util.zip.ZipEntry

open class ZipOutputArchive(stepConfig: XProcStepConfiguration): OutputArchive(stepConfig) {
    override val archiveFormat = Ns.zip
    lateinit var zipStream: ZipArchiveOutputStream

    override fun create(file: File?) {
        if (file == null) {
            archiveFile = createArchive(".zip")
        } else {
            archiveFile = file.toPath()
        }

        zipStream = ZipArchiveOutputStream(archiveFile)
    }

    override fun close() {
        zipStream.close()
    }

    override fun write(entry: XArchiveEntry) {
        val zipEntry = ZipArchiveEntry(entry.name)

        entry.comment?.let { zipEntry.comment = it }

        fileTime(entry.properties[NsCx.fileCreationTime])?.let { zipEntry.creationTime = it }
        entry.properties[NsCx.externalAttributes]?.let { zipEntry.externalAttributes = it.toLong() }
        // setGeneralPurposeBit()
        fileTime(entry.properties[NsCx.lastAccessTime])?.let { zipEntry.lastAccessTime = it }
        fileTime(entry.properties[NsCx.lastModified])?.let { zipEntry.lastModifiedTime = it }
        entry.properties[NsCx.mode]?.let { zipEntry.unixMode = it.toInt() }

        when (entry.method) {
            "none" -> zipEntry.method = ZipEntry.STORED
            "deflated" -> zipEntry.method = ZipEntry.DEFLATED
            else -> Unit
        }

        zipStream.putArchiveEntry(zipEntry)
        entry.write(zipStream)
        zipStream.closeArchiveEntry()

        super.write(entry)
    }


}