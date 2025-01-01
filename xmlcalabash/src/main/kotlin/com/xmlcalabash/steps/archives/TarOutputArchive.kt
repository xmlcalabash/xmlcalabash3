package com.xmlcalabash.steps.archives

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.attribute.FileTime
import java.time.Instant

open class TarOutputArchive(stepConfig: XProcStepConfiguration): OutputArchive(stepConfig) {
    override val archiveFormat = Ns.tar
    lateinit var tarStream: TarArchiveOutputStream

    override fun create(file: File?) {
        if (file == null) {
            archiveFile = createArchive(".tar")
        } else {
            archiveFile = file.toPath()
        }

        val stream = BufferedOutputStream(FileOutputStream(archiveFile.toFile()))
        tarStream = TarArchiveOutputStream(stream)
    }

    override fun close() {
        tarStream.close()
    }

    override fun write(entry: XArchiveEntry) {
        val output = ByteArrayOutputStream()
        entry.write(output)

        val linkFlag = if (entry.properties[NsCx.linkName] != null) {
            if (entry.properties[NsCx.symbolicLink] == "true") {
                TarConstants.LF_SYMLINK
            } else if (entry.properties[NsCx.link] == "true") {
                TarConstants.LF_LINK
            } else {
                TarConstants.LF_NORMAL
            }
        } else {
            TarConstants.LF_NORMAL
        }

        val tarEntry = if (linkFlag != TarConstants.LF_NORMAL) {
            TarArchiveEntry(entry.name, linkFlag)
        } else {
            TarArchiveEntry(entry.name)
        }

        if (linkFlag == TarConstants.LF_NORMAL) {
            tarEntry.setSize(output.size().toLong())
        } else {
            tarEntry.setLinkName(entry.properties[NsCx.linkName]!!)
        }

        entry.properties[NsCx.groupId]?.toLong()?.let { tarEntry.setGroupId(it) }
        entry.properties[NsCx.groupName]?.let { tarEntry.setGroupName(it) }
        entry.properties[NsCx.userId]?.toLong()?.let { tarEntry.setUserId(it) }
        entry.properties[NsCx.userName]?.let { tarEntry.setUserName(it) }
        entry.properties[NsCx.devMajor]?.toInt()?.let { tarEntry.setDevMajor(it) }
        entry.properties[NsCx.devMinor]?.toInt()?.let { tarEntry.setDevMinor(it) }
        entry.properties[NsCx.mode]?.toInt()?.let { tarEntry.setMode(it) }
        entry.properties[NsCx.fileCreationTime]?.let { tarEntry.creationTime = FileTime.from(Instant.parse(it)) }
        entry.properties[NsCx.lastAccessTime]?.let { tarEntry.lastAccessTime = FileTime.from(Instant.parse(it)) }
        entry.properties[NsCx.lastModified]?.let { tarEntry.lastModifiedTime = FileTime.from(Instant.parse(it)) }
        entry.properties[NsCx.statusChangeTime]?.let { tarEntry.statusChangeTime  = FileTime.from(Instant.parse(it)) }

        tarStream.putArchiveEntry(tarEntry)

        if (linkFlag == TarConstants.LF_NORMAL) {
            tarStream.write(output.toByteArray())
        }

        tarStream.closeArchiveEntry()

        super.write(entry)
    }
}