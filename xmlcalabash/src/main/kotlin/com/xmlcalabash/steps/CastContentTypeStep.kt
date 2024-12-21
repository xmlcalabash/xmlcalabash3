package com.xmlcalabash.steps

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.*
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.io.ContentTypeConverter
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.s9api.*
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.*

open class CastContentTypeStep(): AbstractAtomicStep() {
    lateinit var document: XProcDocument
    var docContentType = MediaType.ANY
    var contentType = MediaType.ANY
    var parameters = mapOf<QName,XdmValue>()

    override fun run() {
        super.run()
        document = queues["source"]!!.first()

        docContentType = document.contentType ?: MediaType.OCTET_STREAM
        contentType = mediaTypeBinding(Ns.contentType)
        parameters = qnameMapBinding(Ns.parameters)

        if (contentType == docContentType) {
            receiver.output("result", document)
            return
        }

        val result = when (docContentType.classification()) {
            MediaType.XML -> castFromXml()
            MediaType.HTML -> castFromHtml()
            MediaType.JSON -> castFromJson()
            MediaType.TEXT -> castFromText()
            else -> castFromOther()
        }

        receiver.output("result", result)
    }

    private fun castFromXml(): XProcDocument {
        when (contentType.classification()) {
            MediaType.XML -> return document.with(contentType)
            MediaType.HTML -> return document.with(contentType, true)
            MediaType.JSON -> {
                val root = S9Api.documentElement(document.value as XdmNode)
                if (root.nodeName == NsFn.map) {
                    val result = castToJson(root)
                    return result
                } else if (root.nodeName == NsC.paramSet) {
                    var map = XdmMap()
                    for (child in root.axisIterator(Axis.CHILD)) {
                        if (child.nodeKind == XdmNodeKind.ELEMENT && child.nodeName == NsC.param) {
                            val key = child.getAttributeValue(Ns.name)
                            val value = child.getAttributeValue(Ns.value)
                            if (key != null && value != null) {
                                val qname = QName(key, child)
                                map = map.put(XdmAtomicValue(qname), XdmAtomicValue(value))
                            }
                        }
                    }
                    return XProcDocument.ofJson(map, document.context, contentType, patchProperties(true))
                }
                throw stepConfig.exception(XProcError.xiNotImplemented("xml to json"))
            }
            MediaType.TEXT -> return ContentTypeConverter.toText(stepConfig, document, contentType)
            else -> {
                val node = S9Api.documentElement(document.value as XdmNode)
                if (node.nodeName == NsC.data) {
                    val dataContentTypeAttr = node.getAttributeValue(Ns.contentType)
                        ?: throw stepConfig.exception(XProcError.xcContentTypeRequired())
                    val dataContentType = MediaType.parse(dataContentTypeAttr)
                    if (contentType != dataContentType) {
                        throw stepConfig.exception(XProcError.xcDifferentContentTypes(contentType, dataContentType))
                    }

                    val bytes = try {
                        Base64.getDecoder().decode(node.stringValue)
                    } catch (ex: IllegalArgumentException) {
                        throw stepConfig.exception(XProcError.xcNotBase64(ex.message ?: "Base64 decoding error"))
                    }

                    return XProcDocument.ofBinary(bytes, document.context, contentType, patchProperties(true))
                } else {
                    throw stepConfig.exception(XProcError.xiNotImplemented("xml to other"))
                }
            }
        }
    }

    private fun castFromHtml(): XProcDocument {
        when (contentType.classification()) {
            MediaType.XML -> return document.with(contentType, true)
            MediaType.HTML -> return document.with(contentType)
            MediaType.JSON -> throw stepConfig.exception(XProcError.xiNotImplemented("html to json"))
            MediaType.TEXT ->  return ContentTypeConverter.toText(stepConfig, document, contentType)
            else -> {
                throw stepConfig.exception(XProcError.xiNotImplemented("html to other"))
            }
        }
    }

    private fun castFromJson(): XProcDocument {
        val classification = contentType.classification()
        when (classification) {
            MediaType.XML -> {
                return ContentTypeConverter.jsonToXml(stepConfig, document, contentType)
            }
            MediaType.HTML -> {
                throw stepConfig.exception(XProcError.xiNotImplemented("json to html"))
            }
            MediaType.JSON -> return document.with(contentType)
            MediaType.TEXT -> return ContentTypeConverter.toText(stepConfig, document, contentType)
            else -> {
                throw stepConfig.exception(XProcError.xiNotImplemented("json to other"))
            }
        }
    }

    private fun castToJson(fnXml: XdmNode): XProcDocument {
        // Around the houses the other way...
        var compiler = stepConfig.processor.newXPathCompiler()
        compiler.declareVariable(QName("a"))
        var selector = compiler.compile("xml-to-json(\$a)").load()
        selector.resourceResolver = stepConfig.environment.documentManager
        selector.setVariable(QName("a"), document.value)
        var result = selector.evaluate()

        compiler = stepConfig.processor.newXPathCompiler()
        compiler.declareVariable(QName("a"))
        selector = compiler.compile("parse-json(\$a)").load()
        selector.resourceResolver = stepConfig.environment.documentManager
        selector.setVariable(QName("a"), result)
        result = selector.evaluate()

        return XProcDocument.ofJson(result, stepConfig, contentType, patchProperties(true))
    }

    private fun castFromText(): XProcDocument {
        when (contentType.classification()) {
            MediaType.XML -> {
                try {
                    return ContentTypeConverter.toXml(stepConfig, document, contentType)
                } catch (ex: Exception) {
                    throw stepConfig.exception(XProcError.xdNotWellFormed())
                }
            }
            MediaType.HTML -> {
                try {
                    return ContentTypeConverter.toXml(stepConfig, document, contentType)
                } catch (ex: Exception) {
                    throw stepConfig.exception(XProcError.xdNotWellFormed())
                }
            }
            MediaType.JSON -> {
                try {
                    return ContentTypeConverter.toJson(stepConfig, document, contentType, document.properties, parameters)
                        .with(contentType, true)
                } catch (ex: Exception) {
                    throw stepConfig.exception(XProcError.xdNotWellFormedJson())
                }
            }
            MediaType.TEXT -> return document.with(contentType)
            else -> {
                throw stepConfig.exception(XProcError.xiNotImplemented("text to other"))
            }
        }
    }

    private fun castFromOther(): XProcDocument {
        when (contentType.classification()) {
            MediaType.XML -> {
                val binaryValue = (document as XProcBinaryDocument).binaryValue

                val builder = SaxonTreeBuilder(stepConfig)
                builder.startDocument(document.baseURI)

                val amap = mutableMapOf(
                    Ns.contentType to docContentType.toString(),
                    Ns.encoding to "base64"
                )
                builder.addStartElement(NsC.data, stepConfig.attributeMap(amap))
                builder.addText(Base64.getEncoder().encodeToString(binaryValue))
                builder.addEndElement()
                builder.endDocument()
                return document.with(builder.result).with(contentType, true)
            }
            else -> {
                if (contentType == MediaType.OCTET_STREAM) {
                    return document.with(contentType)
                }
                throw stepConfig.exception(XProcError.xiNotImplemented("other to ${contentType}"))
            }
        }
    }

    private fun patchProperties(removeSerialization: Boolean = false): DocumentProperties {
        val newProps = DocumentProperties(document.properties)
        newProps.remove(Ns.contentType)
        if (removeSerialization) {
            newProps.remove(Ns.serialization)
        }
        return newProps
    }

    override fun toString(): String = "p:cast-content-type"
}