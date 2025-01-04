package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import org.apache.commons.compress.compressors.CompressorInputStream
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.z.ZCompressorInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.Exception

open class UncompressStep(): AbstractAtomicStep() {
    companion object {
        fun uncompress(stepConfig: XProcStepConfiguration, format: QName, inputStream: InputStream): ByteArrayOutputStream {
            val baos = ByteArrayOutputStream()
            try {
                val decompressor: CompressorInputStream = when (format) {
                    Ns.gzip -> GzipCompressorInputStream(inputStream)
                    Ns.bzip2 -> BZip2CompressorInputStream(inputStream)
                    Ns.brotli -> BrotliCompressorInputStream(inputStream)
                    Ns.deflate -> DeflateCompressorInputStream(inputStream)
                    Ns.lzma -> LZMACompressorInputStream(inputStream)
                    Ns.xz -> XZCompressorInputStream(inputStream)
                    Ns.compress -> ZCompressorInputStream(inputStream)
                    else -> throw stepConfig.exception(XProcError.xcUnsupportedCompressionFormat(format))
                }

                val buf = ByteArray(8192)
                var len = decompressor.read(buf)
                while (len >= 0) {
                    baos.write(buf, 0, len)
                    len = decompressor.read(buf)
                }

                decompressor.close()
            } catch (ex: XProcException) {
                throw ex
            } catch (ex: Exception) {
                throw stepConfig.exception(XProcError.xcCannotUncompress(), ex)
            }

            return baos
        }
    }

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val parameters = qnameMapBinding(Ns.parameters)
        val outputContentType = mediaTypeBinding(Ns.contentType)

        if (document !is XProcBinaryDocument) {
            throw stepConfig.exception(XProcError.xcCannotUncompress())
        }

        val inputStream = BufferedInputStream(ByteArrayInputStream(document.binaryValue))

        val contentType = document.contentType ?: MediaType.OCTET_STREAM

        val declFormat = qnameBinding(Ns.format)
        val format = if (declFormat != null) {
            declFormat
        } else if (contentType.mediaType != "application") {
            throw stepConfig.exception(XProcError.xcCannotUncompress(contentType))
        } else {
            when (contentType.mediaSubtype) {
                "gzip" -> Ns.gzip
                "bzip2" -> Ns.bzip2
                "brotli" -> Ns.brotli
                "deflate" -> Ns.deflate
                "lzma" -> Ns.lzma
                "xz" -> Ns.xz
                "compress" -> Ns.compress
                else -> throw stepConfig.exception(XProcError.xcCannotUncompress(contentType))
            }
        }

        val baos = uncompress(stepConfig, format, inputStream)

        val loader = DocumentLoader(stepConfig, document.baseURI, document.properties, parameters)
        try {
            val result = loader.load(document.baseURI, ByteArrayInputStream(baos.toByteArray()), outputContentType)

            val properties = DocumentProperties(document.properties)
            properties.remove(Ns.serialization)
            properties[Ns.contentType] = outputContentType

            receiver.output("result", result.with(properties))
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcCannotUncompress(outputContentType), ex)
        }
    }

    override fun toString(): String = "p:uncompress"
}