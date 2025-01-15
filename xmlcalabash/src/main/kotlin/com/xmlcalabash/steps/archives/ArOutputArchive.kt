package com.xmlcalabash.steps.archives

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.FileUtils
import org.apache.commons.compress.archivers.ar.ArArchiveEntry
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.zip.ZipEntry

open class ArOutputArchive(stepConfig: XProcStepConfiguration): OutputArchive(stepConfig) {
    override val archiveFormat = Ns.ar
    lateinit var arStream: ArArchiveOutputStream

    override fun create(file: File?) {
        if (file == null) {
            archiveFile = createArchive(".a")
        } else {
            archiveFile = file.toPath()
        }

        val stream = BufferedOutputStream(FileUtils.outputStream(archiveFile))
        arStream = ArArchiveOutputStream(stream)
    }

    override fun close() {
        arStream.close()
    }

    override fun write(entry: XArchiveEntry) {
        val output = ByteArrayOutputStream()
        entry.write(output)

        val userId = entry.properties[NsCx.userId]?.toInt() ?: 0
        val groupId = entry.properties[NsCx.groupId]?.toInt() ?: 0
        val mode = entry.properties[NsCx.mode]?.toInt() ?: 0

        val lastModified = if (entry.properties[NsCx.lastModified] != null) {
            val instant = Instant.parse(entry.properties[NsCx.lastModified]!!)
            instant.toEpochMilli() / 1000
        } else {
            0
        }

        val arEntry = ArArchiveEntry(entry.name, output.size().toLong(), userId, groupId, mode, lastModified)

        arStream.putArchiveEntry(arEntry)
        arStream.write(output.toByteArray())
        arStream.closeArchiveEntry()

        super.write(entry)
    }
}