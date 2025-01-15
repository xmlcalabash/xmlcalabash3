package com.xmlcalabash.steps.archives

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.FileUtils
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream
import org.apache.commons.compress.archivers.cpio.CpioConstants
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

open class CpioOutputArchive(stepConfig: XProcStepConfiguration): OutputArchive(stepConfig) {
    override val archiveFormat = Ns.cpio
    lateinit var cpioStream: CpioArchiveOutputStream

    override fun create(file: File?) {
        if (file == null) {
            archiveFile = createArchive(".cpio")
        } else {
            archiveFile = file.toPath()
        }

        val stream = BufferedOutputStream(FileUtils.outputStream(archiveFile))
        cpioStream = CpioArchiveOutputStream(stream)
    }

    override fun close() {
        cpioStream.close()
    }

    override fun write(entry: XArchiveEntry) {
        val output = ByteArrayOutputStream()
        entry.write(output)

        val cpioEntry = CpioArchiveEntry(entry.name, output.size().toLong())

        val newMask = CpioConstants.FORMAT_NEW.toInt() or CpioConstants.FORMAT_NEW_CRC.toInt() or CpioConstants.FORMAT_NEW_MASK.toInt()
        if (cpioEntry.format.toInt() and newMask == 0) {
            entry.properties[NsCx.device]?.toLong()?.let { cpioEntry.setDevice(it) }
        }

        entry.properties[NsCx.mode]?.toLong()?.let { cpioEntry.setMode(it) }
        entry.properties[NsCx.devMajor]?.toLong()?.let { cpioEntry.setDeviceMaj(it) }
        entry.properties[NsCx.devMinor]?.toLong()?.let { cpioEntry.setDeviceMin(it) }
        entry.properties[NsCx.groupId]?.toLong()?.let { cpioEntry.setGID(it) }
        entry.properties[NsCx.userId]?.toLong()?.let { cpioEntry.setUID(it) }

        cpioStream.putArchiveEntry(cpioEntry)
        cpioStream.write(output.toByteArray())
        cpioStream.closeArchiveEntry()

        super.write(entry)
    }
}