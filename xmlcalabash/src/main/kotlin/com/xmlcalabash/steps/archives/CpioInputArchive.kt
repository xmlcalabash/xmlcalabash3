package com.xmlcalabash.steps.archives

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream
import org.apache.commons.compress.archivers.cpio.CpioConstants
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.attribute.FileTime
import java.time.Instant
import kotlin.io.path.inputStream

open class CpioInputArchive(stepConfig: XProcStepConfiguration, doc: XProcBinaryDocument): InputArchive(stepConfig, doc) {
    override val archiveFormat = Ns.cpio
    override val baseUri = doc.baseURI
    lateinit var archive: CpioArchiveInputStream

    protected fun openCpio() {
        openArchive(".cpio")
        archive = CpioArchiveInputStream(BufferedInputStream(archiveFile.inputStream()))
        var index = 0
        for (entry in archive) {
            if (entry.isDirectory) {
                continue
            }

            val archiveEntry = XArchiveEntry(stepConfig, entry.name, entry, this)
            val amap = mutableMapOf<QName, String>(
                Ns.name to entry.name,
                Ns.contentType to "${MediaType.parse(stepConfig.environment.mimeTypes.getContentType(entry.name))}",
                NsCx.size to "${entry.size}"
            )

            val cpioFormat = entry.format
            when (cpioFormat) {
                CpioConstants.FORMAT_NEW -> amap[NsCx.format] = "new"
                CpioConstants.FORMAT_NEW_CRC -> amap[NsCx.format] = "new-crc"
                CpioConstants.FORMAT_NEW_MASK -> amap[NsCx.format] = "new-mask"
                CpioConstants.FORMAT_OLD_ASCII -> amap[NsCx.format] = "old-ascii"
                CpioConstants.FORMAT_OLD_BINARY -> amap[NsCx.format] = "old-binary"
                CpioConstants.FORMAT_OLD_MASK -> amap[NsCx.format] = "old-mask"
                else -> amap[NsCx.format] = "unknown"
            }

            val newMask = CpioConstants.FORMAT_NEW.toInt() or CpioConstants.FORMAT_NEW_CRC.toInt() or CpioConstants.FORMAT_NEW_MASK.toInt()
            if (cpioFormat.toInt() and newMask != 0) {
                amap[NsCx.checkSum] = "${entry.getChksum()}"
                amap[NsCx.devMajor] = "${entry.deviceMaj}"
                amap[NsCx.devMinor] = "${entry.deviceMaj}"
                amap[NsCx.remoteDevMajor] = "${entry.remoteDeviceMaj}"
                amap[NsCx.remoteDevMinor] = "${entry.remoteDeviceMin}"
            } else {
                amap[NsCx.device] = "${entry.device}"
                if (entry.remoteDevice != 0L) {
                    amap[NsCx.remoteDevice] = "${entry.remoteDevice}"
                }
            }

            amap[NsCx.groupId] = "${entry.gid}"
            amap[NsCx.userId] = "${entry.uid}"
            amap[NsCx.lastModified] = iso8601(FileTime.from(entry.lastModifiedDate.toInstant()))
            amap[NsCx.mode] = "${entry.mode}"
            amap[NsCx.modeString] = unixModeString(entry.mode.toInt())
            if (entry.numberOfLinks != 1L) {
                amap[NsCx.numberOfLinks] = "${entry.numberOfLinks}"
            }
            amap[NsCx.time] = iso8601(FileTime.from(Instant.ofEpochMilli(entry.time * 1000)))

            if (entry.isBlockDevice) { amap[NsCx.deviceType] = "block-device" }
            if (entry.isCharacterDevice) { amap[NsCx.deviceType] = "character-device" }
            if (entry.isDirectory) { amap[NsCx.deviceType] = "directory" }
            if (entry.isNetwork) { amap[NsCx.deviceType] = "network" }
            if (entry.isPipe) { amap[NsCx.deviceType] = "pipe" }
            if (entry.isSocket) { amap[NsCx.deviceType] = "socket" }
            if (entry.isSymbolicLink) { amap[NsCx.deviceType] = "symbolic-link" }

            archiveEntry.properties.putAll(amap)
            archiveEntry.position = index++
            _entries.add(archiveEntry)
        }
        archive.close()
    }

    override fun open() {
        try {
            openCpio()
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(Ns.cpio), ex)
        }
    }

    override fun close() {
        _entries.clear()
    }

    override fun inputStream(entry: XArchiveEntry): InputStream {
        // I can't see any other way to do this...I could be clever about keeping the
        // stream open and all, but it doesn't seem worth it
        archive = CpioArchiveInputStream(BufferedInputStream(archiveFile.inputStream()))

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