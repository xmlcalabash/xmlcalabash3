package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.archives.ArInputArchive
import com.xmlcalabash.steps.archives.ArjInputArchive
import com.xmlcalabash.steps.archives.CpioInputArchive
import com.xmlcalabash.steps.archives.InputArchive
import com.xmlcalabash.steps.archives.JarInputArchive
import com.xmlcalabash.steps.archives.SevenZInputArchive
import com.xmlcalabash.steps.archives.TarInputArchive
import com.xmlcalabash.steps.archives.ZipInputArchive
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

    override fun run() {
        super.run()

        val archive = queues["source"]!!.first()

        val format = qnameBinding(Ns.format) ?: Ns.zip

        if (archive !is XProcBinaryDocument) {
            // If it isn't binary, it definitely doesn't match the format...
            throw stepConfig.exception(XProcError.xcArchiveFormatIncorrect(format))
        }

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
            Ns.zip -> archiveManifest(builder, "zip", ZipInputArchive(stepConfig, archive))
            Ns.jar -> archiveManifest(builder, "jar", JarInputArchive(stepConfig, archive))
            Ns.tar -> archiveManifest(builder, "tar", TarInputArchive(stepConfig, archive))
            Ns.ar -> archiveManifest(builder, "ar", ArInputArchive(stepConfig, archive))
            Ns.arj -> archiveManifest(builder, "arj", ArjInputArchive(stepConfig, archive))
            Ns.cpio -> archiveManifest(builder, "cpio", CpioInputArchive(stepConfig, archive))
            Ns.sevenZ -> archiveManifest(builder, "7z", SevenZInputArchive(stepConfig, archive))
            else -> throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(format))
        }

        builder.endDocument()

        val result = builder.result

        receiver.output("result", XProcDocument.ofXml(result, stepConfig))
    }

    private fun archiveManifest(builder: SaxonTreeBuilder, format: String, archive: InputArchive) {
        archive.open()
        builder.addStartElement(NsC.archive, stepConfig.typeUtils.attributeMap(mapOf(Ns.format to format)))
        for (entry in archive.entries) {
            val amap = mutableMapOf<QName, String>()
            amap.putAll(entry.properties)
            amap[Ns.href] = "${relativeTo.resolve(entry.name)}"
            amap[Ns.contentType] = "${contentType(entry.name)}"
            builder.addStartElement(NsC.entry, stepConfig.typeUtils.attributeMap(amap))
            builder.addEndElement()
        }
        builder.addEndElement()
        archive.close()
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