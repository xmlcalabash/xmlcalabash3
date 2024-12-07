package com.xmlcalabash.steps.internal

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.parameters.InlineStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

open class InlineStep(val params: InlineStepParameters): AbstractAtomicStep() {
    var contextItem: XProcDocument? = null
    private var contextSequence = false

    override fun input(port: String, doc: XProcDocument) {
        contextSequence = contextItem != null
        contextItem = doc
    }

    override fun run() {
        if (contextSequence && !params.filter.isStatic()) {
            throw XProcError.xdInlineContextSequence().at(stepParams.location).exception()
        }

        super.run()

        val xml = if (params.filter.isStatic()) {
            params.filter.getNode()
        } else {
            try {
                params.filter.expandValueTemplates(contextItem, options)
            } catch (ex: SaxonApiException) {
                if (ex.message != null && ex.message!!.contains("Namespace prefix") && ex.message!!.contains("has not been declared")) {
                    throw XProcError.xdNoBindingInScope(ex.message!!).exception()
                }
                throw XProcError.xsXPathStaticError(ex.message ?: "").exception(ex)
            }
        }

        val docProps = options[Ns.documentProperties]?.value ?: XdmMap()
        val docMap = if (docProps === XdmEmptySequence.getInstance()) {
            XdmMap()
        } else {
            try {
                stepConfig.forceQNameKeys(docProps as XdmMap)
            } catch (ex: Exception) {
                throw XProcError.xdInvalidSerialization(docProps.toString()).exception()
            }
        }
        val props = DocumentProperties(stepConfig.asMap(docMap))

        val ptype = props[Ns.contentType]
        if (ptype != null) {
            val pmtype = MediaType.parse(ptype.underlyingValue.stringValue)
            if (pmtype != params.contentType) {
                throw XProcError.xdContentTypesDiffer(params.contentType.toString(), pmtype.toString()).exception()
            }
        } else {
            props[Ns.contentType] = params.contentType
        }

        val ctype = MediaType.parse(props[Ns.contentType]!!.underlyingValue.stringValue)

        // This is about whether the original inline contains markup
        if (params.filter.containsMarkup() && !(ctype.xmlContentType() || ctype.htmlContentType())) {
            throw XProcError.xdMarkupForbidden(ctype.toString()).exception()
        }

        // This is about whether the constructed output contains markup
        val markup = containsMarkup(xml)

        if (params.encoding == null) {
            if (ctype.charset() != null) {
                throw XProcError.xdEncodingRequired(ctype.charset()!!).exception()
            }
        } else {
            if (ctype.xmlContentType() || ctype.htmlContentType()) {
                throw XProcError.xdEncodingWithXmlOrHtml(params.encoding).exception()
            }
            if (markup) {
                throw XProcError.xdMarkupForbiddenWithEncoding(params.encoding).exception()
            }
        }

        if (!props.has(Ns.baseUri)) {
            props[Ns.baseUri] = xml.baseURI
        }

        // Handle the special case where the baseURI is ()
        val pbaseUri = props[Ns.baseUri]
        if (pbaseUri == null || pbaseUri == XdmEmptySequence.getInstance()) {
            props.remove(Ns.baseUri)
        }

        if (markup) {
            if (ctype.textContentType()) {
                receiver.output("result", XProcDocument.ofText(xml.stringValue, stepConfig, ctype, props))
                return
            } else {
                if (!(ctype.xmlContentType() || ctype.htmlContentType())) {
                    throw XProcError.xdMarkupForbidden(ctype.toString()).exception()
                }
            }
        }

        if (ctype.xmlContentType() || ctype.htmlContentType()) {
            val fixedXML = S9Api.adjustBaseUri(xml, props[Ns.baseUri])
            receiver.output("result", XProcDocument(fixedXML, stepConfig, props))
            return
        }

        val bytes = decode(xml.stringValue, params.encoding)

        if (ctype.jsonContentType()) {
            val json = parseJson(bytes, ctype)
            receiver.output("result", XProcDocument(json, stepConfig, props))
            return
        }

        if (ctype.textContentType()) {
            val baseURI = if (props.has(Ns.baseUri)) {
                props.getUri(Ns.baseUri)
            } else {
                stepConfig.baseUri
            }

            val charsetName = ctype.charset() ?: "UTF-8"
            if (!Charset.isSupported(charsetName)) {
                throw XProcError.xdUnsupportedCharset(charsetName).exception()
            }

            val charset = Charset.forName(ctype.charset() ?: "UTF-8")
            val builder = SaxonTreeBuilder(stepConfig.processor)
            builder.startDocument(baseURI)
            builder.addText(bytes.toString(charset))
            builder.endDocument()
            receiver.output("result", XProcDocument(builder.result, stepConfig, props))
            return
        }

        receiver.output("result", XProcDocument.ofBinary(bytes, stepConfig, props))
    }

    override fun reset() {
        super.reset()
        contextItem = null
        contextSequence = false
    }

    private fun parseJson(bytes: ByteArray, contentType: MediaType?): XdmValue {
        val charset = Charset.forName(contentType?.charset() ?: "UTF-8")
        val text = bytes.toString(charset)

        try {
            val compiler = stepConfig.processor.newXPathCompiler()
            compiler.declareVariable(QName("a"))
            val selector = compiler.compile("parse-json(\$a)").load()
            selector.resourceResolver = stepConfig.documentManager
            selector.setVariable(QName("a"), XdmAtomicValue(text))
            val result = selector.evaluate()
            return result
        } catch (ex: SaxonApiException) {
            throw XProcError.xdNotWellFormedJson().exception(ex)
        }
    }

    private fun decode(text: String, encoding: String?): ByteArray {
        if (encoding == null) {
            return text.toByteArray(StandardCharsets.UTF_8)
        }

        // Assume whitespace is not part of the encoding (it's not part of base64)
        val cleanText = text.replace("\\s+".toRegex(), "")

        try {
            // It's a static error if the encoding isn't base64
            val decoder = Base64.getDecoder()
            val bytes = decoder.decode(cleanText)
            return bytes
        } catch (ex: IllegalArgumentException) {
            throw XProcError.xdBadBase64Input().exception(ex)
        }
    }

    private fun containsMarkup(xml: XdmNode): Boolean {
        when (xml.nodeKind) {
            XdmNodeKind.DOCUMENT -> {
                for (child in xml.axisIterator(Axis.CHILD)) {
                    if (containsMarkup(child)) {
                        return true
                    }
                }
                return false
            }
            XdmNodeKind.TEXT -> return false
            else -> return true
        }
    }

    override fun toString(): String = "cx:inline"
}