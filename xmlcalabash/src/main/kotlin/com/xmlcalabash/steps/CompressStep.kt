package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.value.HexBinaryValue
import org.apache.commons.compress.compressors.CompressorOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

open class CompressStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()
        val document = queues["source"]!!.first()

        val parameters = qnameMapBinding(Ns.parameters)
        val serialization = qnameMapBinding(Ns.serialization)

        val doc = document
        val format = qnameBinding(Ns.format) ?: Ns.gzip

        val bytes = if (doc.value.underlyingValue is HexBinaryValue) {
            val hexbin = doc.value.underlyingValue as HexBinaryValue
            hexbin.binaryValue
        } else {
            val baos = ByteArrayOutputStream()
            DocumentWriter(doc, baos).write()
            baos.toByteArray()
        }

        val baos = ByteArrayOutputStream()
        val buf = BufferedOutputStream(baos)

        val compressor: CompressorOutputStream<*> = when (format) {
            Ns.gzip -> GzipCompressorOutputStream(buf)
            Ns.bzip2 -> BZip2CompressorOutputStream(buf)
            Ns.deflate -> DeflateCompressorOutputStream(buf)
            Ns.lzma -> LZMACompressorOutputStream(buf)
            Ns.xz -> XZCompressorOutputStream(buf)
            else -> throw stepConfig.exception(XProcError.xcUnsupportedCompressionFormat(format))
        }

        compressor.write(bytes, 0, bytes.size)
        compressor.close()

        val properties = DocumentProperties(doc.properties)
        properties.remove(Ns.serialization)
        properties[Ns.contentType] = "application/${format.localName}"

        receiver.output("result", XProcDocument.ofBinary(baos.toByteArray(), stepConfig, properties))
    }

    override fun toString(): String = "p:store"
}