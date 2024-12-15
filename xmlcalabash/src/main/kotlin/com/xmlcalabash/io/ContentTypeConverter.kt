package com.xmlcalabash.io

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*

class ContentTypeConverter() {
    companion object {
        fun toXml(context: XProcStepConfiguration, document: XProcDocument, contentType: MediaType,
                  properties: DocumentProperties = document.properties): XProcDocument {
            val compiler = context.processor.newXPathCompiler()
            compiler.declareVariable(QName("a"))

            val newProps = DocumentProperties(properties)
            newProps.remove(Ns.serialization)
            newProps.remove(Ns.contentType)

            val selector = compiler.compile("parse-xml(\$a)").load()
            selector.setVariable(QName("a"), XdmAtomicValue(document.value.underlyingValue.stringValue))
            selector.resourceResolver = context.environment.documentManager
            val xml = selector.evaluate()
            return XProcDocument.ofXml(xml as XdmNode, context, contentType, newProps)
        }

        fun toText(context: XProcStepConfiguration, document: XProcDocument, contentType: MediaType,
                   properties: DocumentProperties = document.properties): XProcDocument {
            val value = document.value

//            val compiler = context.processor.newXPathCompiler()
//            compiler.declareVariable(QName("a"))
//            compiler.declareVariable(QName("serprop"))

            val newProps = DocumentProperties(properties)
            val serprop = if (newProps.has(Ns.serialization)) {
                val smap = newProps[Ns.serialization] as XdmMap
                var convmap = XdmMap()
                for (key in smap.keySet()) {
                    val name = key.qNameValue
                    if (name.namespaceUri == NamespaceUri.NULL) {
                        convmap = convmap.put(XdmAtomicValue(name.localName), smap.get(key))
                    } else {
                        convmap = convmap.put(key, smap.get(key))
                    }
                }
                convmap
            } else {
                XdmMap().put(XdmAtomicValue(Ns.method), XdmAtomicValue("text"))
            }
            newProps.remove(Ns.serialization)
            newProps.remove(Ns.contentType)

            val text = toText(context, value, serprop as XdmMap)

            val builder = SaxonTreeBuilder(context.processor)
            builder.startDocument(document.baseURI)
            builder.addText(text)
            builder.endDocument()

            val result = builder.result
            return XProcDocument.ofXml(result, context, contentType, newProps)
        }

        private fun toText(context: XProcStepConfiguration, value: XdmValue, serprop: XdmMap): String {
            val compiler = context.processor.newXPathCompiler()
            compiler.declareVariable(QName("a"))
            compiler.declareVariable(QName("serprop"))

            val selector = compiler.compile("serialize(\$a, \$serprop)").load()
            selector.resourceResolver = context.environment.documentManager

            selector.setVariable(QName("a"), value)
            selector.setVariable(QName("serprop"), serprop)
            val text = selector.evaluate()
            return text.underlyingValue.stringValue
        }

        fun toJson(context: XProcStepConfiguration, document: XProcDocument, contentType: MediaType,
                   properties: DocumentProperties, parameters: Map<QName,XdmValue>): XProcDocument {
            val value = document.value

            if (value is XdmNode) {
                val text = if (value.nodeKind == XdmNodeKind.DOCUMENT) {
                    val sb = StringBuilder()
                    for (child in value.axisIterator(Axis.CHILD)) {
                        if (child.nodeKind == XdmNodeKind.TEXT) {
                            sb.append(child.stringValue)
                        } else {
                            throw XProcError.xiCannotCastTo(contentType).exception()
                        }
                    }
                    sb.toString()
                } else {
                    if (value.nodeKind == XdmNodeKind.TEXT) {
                        value.stringValue
                    } else {
                        throw XProcError.xiCannotCastTo(contentType).exception()
                    }
                }
                val compiler = context.processor.newXPathCompiler()
                compiler.declareVariable(QName("a"))
                compiler.declareVariable(QName("param"))
                val selector = compiler.compile("parse-json(\$a, \$param)").load()
                selector.resourceResolver = context.environment.documentManager
                selector.setVariable(QName("a"), XdmAtomicValue(text))
                selector.setVariable(QName("param"), context.asXdmMap(parameters))
                val result = selector.evaluate()

                return XProcDocument.ofJson(result, context, contentType, properties)
            }

            throw XProcError.xiCannotCastTo(contentType).exception()
        }

        fun jsonToXml(context: XProcStepConfiguration, document: XProcDocument, contentType: MediaType): XProcDocument {
            val node = jsonToXml(context, document.value, contentType)
            return XProcDocument.ofXml(node, context, contentType, patchProperties(document, true))
        }

        fun jsonToXml(context: XProcStepConfiguration, value: XdmValue, contentType: MediaType): XdmNode {
            // This feels like going around the houses...
            var compiler = context.processor.newXPathCompiler()
            compiler.declareVariable(QName("a"))
            var selector = compiler.compile("serialize(\$a, map{'method':'json'})").load()
            selector.resourceResolver = context.environment.documentManager
            selector.setVariable(QName("a"), value)
            val result = selector.evaluate()

            compiler = context.processor.newXPathCompiler()
            compiler.declareVariable(QName("a"))
            selector = compiler.compile("json-to-xml(\$a)").load()
            selector.resourceResolver = context.environment.documentManager
            selector.setVariable(QName("a"), result)

            return selector.evaluate() as XdmNode
        }

        private fun patchProperties(document: XProcDocument, removeSerialization: Boolean = false): DocumentProperties {
            val newProps = DocumentProperties(document.properties)
            newProps.remove(Ns.contentType)
            if (removeSerialization) {
                newProps.remove(Ns.serialization)
            }
            return newProps
        }
    }
}