package com.xmlcalabash.steps.archives

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.s9api.QName
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.io.path.inputStream

open class TarInputArchive(stepConfig: StepConfiguration, doc: XProcBinaryDocument): InputArchive(stepConfig, doc) {
    override val archiveFormat = Ns.tar
    override val baseUri = doc.baseURI
    private var gzipped = false

    protected fun openTar() {
        openArchive(".tar")
        var index = 0
        for (entry in readEntries()) {
            val archiveEntry = XArchiveEntry(stepConfig, entry.name, entry, this)
            val amap = mutableMapOf<QName, String>(
                Ns.name to entry.name,
                Ns.contentType to "${MediaType.parse(stepConfig.environment.mimeTypes.getContentType(entry.name))}",
                NsCx.groupId to entry.longGroupId.toString(),
                NsCx.groupName to entry.groupName,
                NsCx.userId to entry.longUserId.toString(),
                NsCx.userName to entry.userName,
                NsCx.size to entry.size.toString()
            )

            if (entry.devMajor != 0) {
                amap[NsCx.devMajor] = entry.devMajor.toString()
            }

            if (entry.devMinor != 0) {
                amap[NsCx.devMinor] = entry.devMinor.toString()
            }

            if (entry.isLink || entry.isSymbolicLink) {
                amap[NsCx.link] = "true"
                if (entry.isSymbolicLink) {
                    amap[NsCx.symbolicLink] = "true"
                }

                val flag = entry.linkFlag.toInt()
                if (flag != 0 && entry.linkFlag.toInt() != '0'.code) {
                    amap[NsCx.linkFlag] = flag.toChar().toString()
                }

                if (entry.linkName != null && entry.linkName.isNotBlank()) {
                    amap[NsCx.linkName] = entry.linkName
                }
            }

            amap[NsCx.mode] = entry.mode.toString()
            amap[NsCx.modeString] = unixModeString(entry.mode)
            if (entry.creationTime != null) {
                amap[NsCx.fileCreationTime] = iso8601(entry.creationTime)
            }
            if (entry.lastAccessTime != null) {
                amap[NsCx.lastAccessTime] = iso8601(entry.lastAccessTime)
            }
            if (entry.lastModifiedTime != null) {
                amap[NsCx.lastModified] = iso8601(entry.lastModifiedTime)
            }
            if (entry.statusChangeTime != null) {
                amap[NsCx.statusChangeTime] = iso8601(entry.statusChangeTime)
            }

            archiveEntry.properties.putAll(amap)
            archiveEntry.position = index++
            _entries.add(archiveEntry)
        }
    }

    private fun readEntries(): List<TarArchiveEntry> {
        // The .tar.gz format is so common, we just make that work if we can.
        val entries = mutableListOf<TarArchiveEntry>()
        val stream = BufferedInputStream(archiveFile.inputStream())
        stream.mark(0)
        try {
            val archive = TarArchiveInputStream(stream)
            for (entry in archive) {
                if (!entry.isDirectory) {
                    entries.add(entry)
                }
            }
        } catch (_: IOException) {
            gzipped = true
            stream.reset()
            val archive = TarArchiveInputStream(GzipCompressorInputStream(stream))
            for (entry in archive) {
                if (!entry.isDirectory) {
                    entries.add(entry)
                }
            }
        }
        return entries
    }

    override fun open() {
        try {
            openTar()
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(Ns.tar), ex)
        }
    }

    override fun close() {
        _entries.clear()
    }

    override fun inputStream(entry: XArchiveEntry): InputStream {
        // I can't see any other way to do this...I could be clever about keeping the
        // stream open and all, but it doesn't seem worth it
        val stream = BufferedInputStream(archiveFile.inputStream())
        val archive = if (gzipped) {
            TarArchiveInputStream(GzipCompressorInputStream(stream))
        } else {
            TarArchiveInputStream(stream)
        }

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