package com.xmlcalabash.steps.extension

import com.thaiopensource.relaxng.input.dtd.DtdInputFormat
import com.thaiopensource.relaxng.input.parse.compact.CompactParseInputFormat
import com.thaiopensource.relaxng.input.parse.sax.SAXParseInputFormat
import com.thaiopensource.relaxng.output.LocalOutputDirectory
import com.thaiopensource.relaxng.output.dtd.DtdOutputFormat
import com.thaiopensource.relaxng.output.rnc.RncOutputFormat
import com.thaiopensource.relaxng.output.rng.RngOutputFormat
import com.thaiopensource.relaxng.output.xsd.XsdOutputFormat
import com.thaiopensource.resolver.AbstractResolver
import com.thaiopensource.resolver.Identifier
import com.thaiopensource.resolver.Input
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.QName
import java.io.File
import java.net.URI
import kotlin.collections.contains

class TrangFilesStep(): AbstractTrangStep() {
    companion object {
        val _sourceSchema = QName("source-schema")
        val _resultSchema = QName("result-schema")
    }

    lateinit var sourceSchema: URI
    lateinit var resultSchema: URI

    override fun run() {
        super.run()

        sourceSchema = uriBinding(_sourceSchema)!!
        if (sourceSchema.scheme != "file") {
            throw stepConfig.exception(XProcError.xcxTrangFileUriRequired())
        }

        resultSchema = uriBinding(_resultSchema)!!
        if (resultSchema.scheme != "file") {
            throw stepConfig.exception(XProcError.xcxTrangFileUriRequired())
        }

        sourceFormat = stringBinding(_sourceFormat) ?: inferredFormat(UriUtils.path(sourceSchema))

        if (sourceFormat == "xsd") {
            throw stepConfig.exception(XProcError.xcxTrangNoXsdSource())
        }

        resultFormat = stringBinding(_resultFormat) ?: inferredFormat(UriUtils.path(resultSchema))

        if (sourceFormat == resultFormat) {
            throw stepConfig.exception(XProcError.xcxTrangIdenticalFormats(sourceFormat!!))
        }

        parseNamespaces()
        parseParameters()

        translate()
    }

    private fun translate() {
        val trangInputFormat = trangInputFormat()
        val trangOutputFormat = trangOutputFormat()

        val inputArray = inputOptions.toTypedArray()
        val outputArray = outputOptions.toTypedArray()

        val resolver = TrangResolver()
        val errorHandler = TrangErrorHandler()

        val sc = trangInputFormat.load(sourceSchema.toString(), inputArray, sourceFormat, errorHandler, resolver)
        val od = LocalOutputDirectory(sourceSchema.toString(), File(UriUtils.path(resultSchema)), ".${resultFormat}", outputEncoding, lineLength, indent)

        try {
            trangOutputFormat.output(sc, od, outputArray, sourceFormat, errorHandler)
        } catch (ex: Throwable) {
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "unknown error occurred"), ex)
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.result)
        builder.addText(resultSchema.toString())
        builder.addEndElement()
        builder.endDocument()
        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig, MediaType.XML))
    }

    private fun inferredFormat(path: String): String {
        val pos = path.lastIndexOf('.')
        val ext = path.substring(pos + 1)
        if (ext in listOf("rng", "rnc", "dtd", "xsd")) {
            return ext
        }
        throw stepConfig.exception(XProcError.xcxTrangCannotDetermineFormat())
    }

    override fun toString(): String = "cx:trang-files"

    inner class TrangResolver(): AbstractResolver() {
        override fun resolve(id: Identifier?, input: Input?) {
            if (id == null || input == null) {
                return
            }

            val href = URI(id.uriReference)
            var resolved = stepConfig.environment.documentManager.lookup(href, URI(id.base))

            if (resolved == href) {
                resolved = stepConfig.environment.documentManager.lookup(UriUtils.resolve(URI(id.base), id.uriReference)!!)
            }

            stepConfig.debug { "Trang: ${id.uriReference} (${id.base}) = ${resolved}" }

            input.uri = resolved.toString()
        }

        override fun open(input: Input?) {
            if (input == null) {
                return
            }

            val uri = URI(input.uri)
            val file = File(UriUtils.path(uri))
            input.byteStream = file.inputStream()
        }
    }
}