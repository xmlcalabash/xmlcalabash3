package com.xmlcalabash.steps.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.runtime.parameters.InlineStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.*
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.util.*

open class InlineStep(val params: InlineStepParameters): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val contextItem = queues["source"]?.firstOrNull()
        val contextSequence = (queues["source"]?.size ?: 0) > 1

        if (contextSequence && !params.filter.isStatic()) {
            throw stepConfig.exception(XProcError.xdInlineContextSequence().at(stepParams.location))
        }

        val xml = if (params.filter.isStatic()) {
            params.filter.getNode()
        } else {
            try {
                params.filter.expandValueTemplates(stepConfig, contextItem, options)
            } catch (ex: SaxonApiException) {
                if (ex.message != null && ex.message!!.contains("Namespace prefix") && ex.message!!.contains("has not been declared")) {
                    throw stepConfig.exception(XProcError.xdNoBindingInScope(ex.message!!))
                }
                throw ex
            }
        }

        val docProps = options[Ns.documentProperties]?.value ?: XdmMap()
        val docMap = if (docProps === XdmEmptySequence.getInstance()) {
            XdmMap()
        } else {
            try {
                stepConfig.typeUtils.forceQNameKeys(docProps as XdmMap)
            } catch (ex: Exception) {
                throw stepConfig.exception(XProcError.xdInvalidSerialization(docProps.toString()))
            }
        }
        val props = DocumentProperties(stepConfig.typeUtils.asMap(docMap))

        val ptype = props[Ns.contentType]
        if (ptype != null) {
            val pmtype = MediaType.parse(ptype.underlyingValue.stringValue)
            if (pmtype != params.contentType) {
                throw stepConfig.exception(XProcError.xdContentTypesDiffer(params.contentType.toString(), pmtype.toString()))
            }
        } else {
            props[Ns.contentType] = params.contentType
        }

        val ctype = MediaType.parse(props[Ns.contentType]!!.underlyingValue.stringValue)
        val ctc = ctype.classification()
        val ctypeMarkup = ctc in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)

        // This is about whether the original inline contains markup
        if (params.filter.containsMarkup(stepConfig) && !ctypeMarkup) {
            throw stepConfig.exception(XProcError.xdMarkupForbidden(ctype.toString()))
        }

        // This is about whether the constructed output contains markup
        val markup = containsMarkup(xml)

        if (params.encoding == null) {
            if (ctype.charset() != null) {
                throw stepConfig.exception(XProcError.xdEncodingRequired(ctype.charset()!!.name()))
            }
        } else {
            if (ctypeMarkup) {
                throw stepConfig.exception(XProcError.xdEncodingWithXmlOrHtml(params.encoding))
            }
            if (markup) {
                throw stepConfig.exception(XProcError.xdMarkupForbiddenWithEncoding(params.encoding))
            }
        }

        if (props.has(Ns.baseUri)) {
            val pbaseUri = props[Ns.baseUri]
            if (pbaseUri != null && pbaseUri != XdmEmptySequence.getInstance()) {
                val uristr = pbaseUri.underlyingValue.stringValue
                try {
                    val uri = URI(uristr)
                    if (!uri.isAbsolute) {
                        throw stepConfig.exception(XProcError.xdInvalidUri(uristr))
                    }
                } catch (ex: URISyntaxException) {
                    throw stepConfig.exception(XProcError.xdInvalidUri(uristr))
                }
            }
        } else {
            props[Ns.baseUri] = xml.baseURI
        }

        // Handle the special case where the baseURI is ()
        val pbaseUri = props[Ns.baseUri]
        if (pbaseUri == null || pbaseUri == XdmEmptySequence.getInstance()) {
            props.remove(Ns.baseUri)
        }

        if (markup) {
            if (ctc == MediaClassification.TEXT) {
                receiver.output("result", XProcDocument.ofText(xml.stringValue, stepConfig, ctype, props))
                return
            } else {
                if (!ctypeMarkup) {
                    throw stepConfig.exception(XProcError.xdMarkupForbidden(ctype.toString()))
                }
            }
        }

        if (ctypeMarkup) {
            val fixedXML = S9Api.adjustBaseUri(xml, props[Ns.baseUri])
            receiver.output("result", XProcDocument(fixedXML, stepConfig, props))
            return
        }

        if (!props.has(Ns.baseUri) && stepConfig.baseUri != null) {
            props[Ns.baseUri] = stepConfig.baseUri
        }

        val bytes = decode(xml.stringValue, params.encoding)

        if (ctc == MediaClassification.TEXT) {
            try {
                ctype.charset()
            } catch (ex: XProcException) {
                if (ex.error.code == NsErr.xd(60)) {
                    throw stepConfig.exception(ex.error.with(NsErr.xd(39)))
                }
                throw stepConfig.exception(ex.error)
            }
        }

        val loader = DocumentLoader(stepConfig, props.baseURI, props)
        val result = loader.load(ByteArrayInputStream(bytes), ctype)
        receiver.output("result", result)
    }

    override fun reset() {
        super.reset()
    }

    private fun parseJson(bytes: ByteArray, contentType: MediaType?): XdmValue {
        val charset = contentType?.charset() ?: StandardCharsets.UTF_8
        val text = bytes.toString(charset)

        try {
            val compiler = stepConfig.newXPathCompiler()
            compiler.declareVariable(QName("a"))
            val selector = compiler.compile("parse-json(\$a)").load()
            selector.resourceResolver = stepConfig.environment.documentManager
            selector.setVariable(QName("a"), XdmAtomicValue(text))
            val result = selector.evaluate()
            return result
        } catch (ex: SaxonApiException) {
            throw stepConfig.exception(XProcError.xdNotWellFormedJson(text), ex)
        }
    }

    private fun parseYaml(bytes: ByteArray, contentType: MediaType?): XdmValue {
        val charset = contentType?.charset() ?: StandardCharsets.UTF_8
        val text = bytes.toString(charset)

        val yamlReader = ObjectMapper(YAMLFactory())
        val obj = yamlReader.readValue(text, Object::class.java)
        val jsonWriter = ObjectMapper()
        val str = jsonWriter.writeValueAsString(obj)
        return parseJson(str.toByteArray(StandardCharsets.UTF_8), contentType)
    }

    private fun parseToml(bytes: ByteArray, contentType: MediaType?): XdmValue {
        val charset = contentType?.charset() ?: StandardCharsets.UTF_8
        val text = bytes.toString(charset)

        val tomlReader = ObjectMapper(TomlFactory())
        val obj = tomlReader.readValue(text, Object::class.java)
        val jsonWriter = ObjectMapper()
        val str = jsonWriter.writeValueAsString(obj)
        return parseJson(str.toByteArray(StandardCharsets.UTF_8), contentType)
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
            throw stepConfig.exception(XProcError.xdBadBase64Input(), ex)
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