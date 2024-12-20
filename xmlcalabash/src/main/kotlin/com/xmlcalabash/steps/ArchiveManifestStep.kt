package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmValue
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.arj.ArjArchiveEntry
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarFile
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry

open class ArchiveManifestStep(): AbstractArchiveStep() {
    private lateinit var relativeTo: URI
    private lateinit var parameters: Map<QName, XdmValue>

    override fun input(port: String, doc: XProcDocument) {
        archives.add(doc)
    }

    override fun run() {
        super.run()

        val archive = archives.first()
        val format = qnameBinding(Ns.format) ?: Ns.zip

        if (relativeTo() == null) {
            if (archive.baseURI == null) {
                throw stepConfig.exception(XProcError.xcNoUnarchiveBaseUri())
            }
            relativeTo = URI(archive.baseURI.toString() + "/")
        } else {
            relativeTo = relativeTo()!!
        }

        parameters = qnameMapBinding(Ns.parameters)

        val props = archive.properties
        val bytes = archiveBytes(archive, format)

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(URI(props.get(Ns.baseUri).toString()))

        when (format) {
            Ns.zip -> {
                builder.addStartElement(NsC.archive, stepConfig.attributeMap(mapOf(Ns.format to "zip")))
                zipManifest(builder, SeekableInMemoryByteChannel(bytes))
            }
            Ns.jar -> {
                builder.addStartElement(NsC.archive, stepConfig.attributeMap(mapOf(Ns.format to "jar")))
                zipManifest(builder, SeekableInMemoryByteChannel(bytes))
            }
            Ns.tar -> {
                builder.addStartElement(NsC.archive, stepConfig.attributeMap(mapOf(Ns.format to "tar")))
                tarManifest(builder, ByteArrayInputStream(bytes))
            }
            Ns.ar -> {
                builder.addStartElement(NsC.archive, stepConfig.attributeMap(mapOf(Ns.format to "ar")))
                arManifest(builder, ByteArrayInputStream(bytes))
            }
            Ns.arj -> {
                builder.addStartElement(NsC.archive, stepConfig.attributeMap(mapOf(Ns.format to "arj")))
                arjManifest(builder, ByteArrayInputStream(bytes))
            }
            Ns._7z, Ns.sevenZ -> {
                builder.addStartElement(NsC.archive, stepConfig.attributeMap(mapOf(Ns.format to "7z")))
                sevenZManifest(builder, SeekableInMemoryByteChannel(bytes))
            }
            else -> throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(format))
        }

        builder.addEndElement()
        builder.endDocument()

        val result = builder.result

        receiver.output("result", XProcDocument.ofXml(result, stepConfig))
    }

    private fun tarManifest(builder: SaxonTreeBuilder, stream: InputStream) {
        var archive = TarArchiveInputStream(stream)

        try {
            archive.nextEntry
        } catch (ex: IOException) {
            try {
                stream.reset()
                archive = TarArchiveInputStream(GzipCompressorInputStream(stream))
            } catch (aex: Exception) {
                throw ex
            }
        }

        for (entry in archive) {
            // Ignore directory entries...
            if (!entry.name.endsWith("/") && entry.linkFlag.toInt() != '5'.code) {
                val amap = mutableMapOf<QName, String?>(
                    Ns.name to entry.name,
                    Ns.href to relativeTo.resolve(entry.name).toString(),
                    Ns.contentType to contentType(entry.name).toString(),
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

                val flag = entry.linkFlag.toInt()
                if (flag != 0 && entry.linkFlag.toInt() != '0'.code) {
                    amap[NsCx.linkFlag] = flag.toChar().toString()
                }

                if (entry.linkName != null && entry.linkName.isNotBlank()) {
                    amap[NsCx.linkName] = entry.linkName
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

                builder.addStartElement(NsC.entry, stepConfig.attributeMap(amap))
                builder.addEndElement()
            }
        }
    }

    private fun zipManifest(builder: SaxonTreeBuilder, channel: SeekableByteChannel) {
        val zbuilder = ZipFile.builder()
        zbuilder.setSeekableByteChannel(channel)
        val archive = zbuilder.get()
        for (entry in archive.entries) {
            // Ignore directory entries...
            if (!entry.name.endsWith("/")) {
                val amap = mutableMapOf<QName, String?>(
                    Ns.name to entry.name,
                    Ns.href to relativeTo.resolve(entry.name).toString(),
                    Ns.contentType to contentType(entry.name).toString(),
                    NsCx.size to entry.size.toString())
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

                when (entry.method) {
                    ZipEntry.STORED -> amap[Ns.method] = "none"
                    ZipEntry.DEFLATED -> amap[Ns.method] ="deflated"
                    else -> Unit
                }

                if (entry.comment != null && entry.comment.isNotEmpty()) {
                    amap[Ns.comment] = entry.comment
                }

                builder.addStartElement(NsC.entry, stepConfig.attributeMap(amap))
                builder.addEndElement()
            }
        }
    }

    private fun sevenZManifest(builder: SaxonTreeBuilder, channel: SeekableByteChannel) {
        val zbuilder = SevenZFile.Builder()
        zbuilder.setSeekableByteChannel(channel)
        if (parameters.containsKey(Ns.password)) {
            zbuilder.setPassword(parameters[Ns.password]!!.underlyingValue.stringValue)
        }
        val archive = zbuilder.get()
        for (entry in archive.entries) {
            // Ignore directory entries...
            if (!entry.isDirectory) {
                val amap = mutableMapOf<QName, String?>(
                    Ns.name to entry.name,
                    Ns.href to relativeTo.resolve(entry.name).toString(),
                    Ns.contentType to contentType(entry.name).toString(),
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

                builder.addStartElement(NsC.entry, stepConfig.attributeMap(amap))
                builder.addEndElement()

            }
        }
    }

    private fun arManifest(builder: SaxonTreeBuilder, stream: InputStream) {
        val archive = ArArchiveInputStream(stream)
        for (entry in archive) {
            // Ignore directory entries...
            if (!entry.isDirectory) {
                val amap = mutableMapOf<QName, String?>(
                    Ns.name to entry.name,
                    Ns.href to relativeTo.resolve(entry.name).toString(),
                    Ns.contentType to contentType(entry.name).toString(),
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

                builder.addStartElement(NsC.entry, stepConfig.attributeMap(amap))
                builder.addEndElement()
            }
        }
    }

    private fun arjManifest(builder: SaxonTreeBuilder, stream: InputStream) {
        val archive = ArjArchiveInputStream(stream)
        for (entry in archive) {
            // Ignore directory entries...
            if (!entry.isDirectory) {
                val amap = mutableMapOf<QName, String?>(
                    Ns.name to entry.name,
                    Ns.href to relativeTo.resolve(entry.name).toString(),
                    Ns.contentType to contentType(entry.name).toString(),
                    NsCx.size to entry.size.toString(),
                    NsCx.windowsAttributes to entry.mode.toString(),
                )
                if (entry.mode != 0) {
                    amap[NsCx.mode] = entry.unixMode.toString()
                    amap[NsCx.modeString] = unixModeString(entry.unixMode)
                }
                if (entry.lastModifiedDate != null) {
                    amap[NsCx.lastModified] = iso8601(FileTime.from(entry.lastModifiedDate.toInstant()))
                }
                when (entry.hostOs) {
                    ArjArchiveEntry.HostOs.AMIGA -> amap[NsCx.hostOs] = "Amiga"
                    ArjArchiveEntry.HostOs.APPLE_GS -> amap[NsCx.hostOs] = "Apple IIGS"
                    ArjArchiveEntry.HostOs.ATARI_ST -> amap[NsCx.hostOs] = "Atari ST"
                    ArjArchiveEntry.HostOs.DOS -> amap[NsCx.hostOs] = "DOS"
                    ArjArchiveEntry.HostOs.MAC_OS -> amap[NsCx.hostOs] = "MacOS"
                    ArjArchiveEntry.HostOs.NEXT -> amap[NsCx.hostOs] = "Next"
                    ArjArchiveEntry.HostOs.OS_2 -> amap[NsCx.hostOs] = "OS/2"
                    ArjArchiveEntry.HostOs.PRIMOS -> amap[NsCx.hostOs] = "PRIMOS"
                    ArjArchiveEntry.HostOs.UNIX -> amap[NsCx.hostOs] = "Unix"
                    ArjArchiveEntry.HostOs.VAX_VMS -> amap[NsCx.hostOs] = "VAX/VMS"
                    ArjArchiveEntry.HostOs.WIN32 -> amap[NsCx.hostOs] = "Windows"
                    ArjArchiveEntry.HostOs.WIN95 -> amap[NsCx.hostOs] = "Windows95"
                    else -> Unit
                }

                builder.addStartElement(NsC.entry, stepConfig.attributeMap(amap))
                builder.addEndElement()
            }
        }
    }

    private fun iso8601(time: FileTime): String {
        val instant = Instant.ofEpochMilli(time.toMillis())
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }

    private fun unixModeString(bits: Int): String {
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

    override fun toString(): String = "p:archive-manifest"
}