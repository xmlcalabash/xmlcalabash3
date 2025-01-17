package com.xmlcalabash.util

import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.*
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*

class ValueTemplateFilterXml(val originalNode: XdmNode, val contentType: MediaType, val baseUri: URI): ValueTemplateFilter {
    private var xmlNode = originalNode
    private var static = true
    private var onlyChecking = false
    private var usesContext = false
    private val usesVariables = mutableSetOf<QName>()
    private val usesFunctions = mutableSetOf<Pair<QName,Int>>()

    private var initialExpand = true
    private val variableBindings = mutableMapOf<QName,XdmValue>()
    private val staticVariableBindings = mutableMapOf<QName,XProcExpression>()
    private val expandText = Stack<Boolean>()
    private val xmlMediaType = contentType.classification() in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)

    private var contextItem: Any? = null

    override fun containsMarkup(config: XProcStepConfiguration): Boolean {
        val compiler = config.newXPathCompiler()
        val exec = compiler.compile("//*")
        val selector = exec.load()
        selector.contextItem = originalNode
        val value = selector.evaluate()
        return value !== XdmEmptySequence.getInstance()
    }

    override fun getNode(): XdmNode {
        return xmlNode
    }

    override fun isStatic(): Boolean {
        return static
    }

    override fun usesContext(): Boolean {
        return usesContext
    }

    override fun usesVariables(): Set<QName> {
        return usesVariables
    }

    override fun usesFunctions(): Set<Pair<QName,Int>> {
        return usesFunctions
    }

    override fun expandStaticValueTemplates(config: XProcStepConfiguration, initialExpand: Boolean, staticBindings: Map<QName, XProcExpression>): XdmNode {
        contextItem = null
        this.initialExpand = initialExpand
        expandText.clear()
        expandText.push(initialExpand)

        staticVariableBindings.clear()
        staticVariableBindings.putAll(staticBindings)

        static = true
        onlyChecking = true
        var builder = SaxonTreeBuilder(config)
        builder.startDocument(baseUri)
        filterValueTemplates(config, builder, originalNode)
        builder.endDocument()

        expandText.pop()

        if (static) {
            val xml = builder.result
            builder = SaxonTreeBuilder(config)
            builder.startDocument(baseUri)
            removeInlineExpandText(config, builder, xml)
            builder.endDocument()
            xmlNode = builder.result
        }

        return xmlNode
    }

    override fun expandValueTemplates(config: XProcStepConfiguration, contextItem: XProcDocument?, bindings: Map<QName, LazyValue>): XdmNode {
        return expandValueTemplatesAny(config, contextItem, bindings)
    }

    private fun expandValueTemplatesAny(config: XProcStepConfiguration, contextItem: XProcDocument?, bindings: Map<QName, LazyValue>): XdmNode {
        this.contextItem = contextItem

        expandText.clear()
        expandText.push(initialExpand)

        variableBindings.clear()
        for ((name, doc) in bindings) {
            variableBindings[name] = doc.value
        }

        onlyChecking = false
        val builder = SaxonTreeBuilder(config)
        builder.startDocument(originalNode.baseURI)
        filterValueTemplates(config, builder, originalNode)
        builder.endDocument()

        expandText.pop()

        return builder.result
    }

    private fun filterValueTemplates(config: XProcStepConfiguration, builder: SaxonTreeBuilder, node: XdmNode) {
        when (node.nodeKind) {
            XdmNodeKind.DOCUMENT -> node.axisIterator(Axis.CHILD).forEach { filterValueTemplates(config, builder, it) }
            XdmNodeKind.ELEMENT -> {
                var expand = expandText.peek()

                val inlineAttribute =  if (node.nodeName.namespaceUri == NsP.namespace) {
                    Ns.expandInlineText
                } else {
                    NsP.inlineExpandText
                }

                // Do the attributes before evaluating the new expand-text value because
                // changing the value only applies to descendants...
                val attrMap = mutableMapOf<QName, String?>()
                if (onlyChecking) {
                    attrMap[NsP.inlineExpandText] = expand.toString()
                }
                node.axisIterator(Axis.ATTRIBUTE).forEach { attr ->
                    if (attr.nodeName != inlineAttribute && attr.nodeName != NsP.inlineExpandText) {
                        val value = if (expand) {
                            considerValueTemplates(config, node, attr.stringValue)
                        } else {
                            attr.stringValue
                        }
                        attrMap[attr.nodeName] = value
                    }
                }

                val newExpand = if (onlyChecking) {
                    node.getAttributeValue(inlineAttribute)
                } else {
                    // The first time through, when we're only checking, we replace all
                    // inline-expand-text attributes with p:inline-expand-text attributes,
                    // unconditionally
                    node.getAttributeValue(NsP.inlineExpandText)
                }

                if (newExpand != null) {
                    if (newExpand == "true" || newExpand == "false") {
                        expand = newExpand == "true"
                    } else {
                        // Set a default so we don't get NPEs below...
                        expand = false
                        throw XProcError.xsInvalidExpandText(newExpand).exception()
                    }
                }

                expandText.push(expand)

                builder.addStartElement(node, config.attributeMap(attrMap))
                node.axisIterator(Axis.CHILD).forEach { filterValueTemplates(config, builder, it) }
                builder.addEndElement()

                expandText.pop()
            }
            XdmNodeKind.TEXT -> {
                if (expandText.peek()) {
                    considerValueTemplates(config, builder, node.parent, node.stringValue)
                } else {
                    addSubtree(builder, node)
                }
            }
            else -> addSubtree(builder, node)
        }
    }

    private fun removeInlineExpandText(config: XProcStepConfiguration, builder: SaxonTreeBuilder, node: XdmNode) {
        when (node.nodeKind) {
            XdmNodeKind.DOCUMENT -> node.axisIterator(Axis.CHILD).forEach { removeInlineExpandText(config, builder, it) }
            XdmNodeKind.ELEMENT -> {
                val attrMap = mutableMapOf<QName, String?>()
                node.axisIterator(Axis.ATTRIBUTE).forEach { attr ->
                    if (attr.nodeName != NsP.inlineExpandText) {
                        attrMap[attr.nodeName] = attr.stringValue
                    }
                }
                builder.addStartElement(node, config.attributeMap(attrMap))
                node.axisIterator(Axis.CHILD).forEach { removeInlineExpandText(config, builder, it) }
                builder.addEndElement()
            }
            else -> addSubtree(builder, node)
        }
    }

    private fun considerValueTemplates(config: XProcStepConfiguration, context: XdmNode, text: String): String {
        val avt = ValueTemplateParser.parse(config, text)

        if (avt.value.size == 1) {
            // There are no value templates in here.
            return avt.value[0]
        }

        //val avtConfig = stepConfig.copy()
        //avtConfig.updateWith(context)

        val sb = StringBuilder()
        for (index in avt.value.indices) {
            if (index % 2 == 0) {
                sb.append(avt.value[index])
            } else {
                val expr = XProcExpression.select(config, avt.value[index])
                for ((name, value) in staticVariableBindings) {
                    expr.setStaticBinding(name, value)
                }

                if (expr.canBeResolvedStatically()) {
                    val value = expr.evaluate(config)
                    sb.append(value.underlyingValue.stringValue)
                } else {
                    if (onlyChecking) {
                        checkValueTemplate(expr, avt.value[index], sb)
                    } else {
                        expr.contextItem = contextItem
                        for ((name, value) in variableBindings) {
                            expr.setBinding(name, value)
                        }
                        val value = expr.evaluate(config)
                        sb.append(value.underlyingValue.stringValue)
                    }
                }
            }
        }

        return sb.toString()
    }

    private fun considerValueTemplates(config: XProcStepConfiguration, builder: SaxonTreeBuilder, context: XdmNode, text: String) {
        val avt = ValueTemplateParser.parse(config, text)

        //val avtConfig = stepConfig.copy()
        //avtConfig.updateWith(context)

        if (avt.value.size == 1) {
            // There are no value templates in here.
            builder.addText(avt.value[0])
            return
        }

        for (index in avt.value.indices) {
            if (index % 2 == 0) {
                builder.addText(avt.value[index])
            } else {
                val expr = XProcExpression.select(config, avt.value[index])
                for ((name, value) in staticVariableBindings) {
                    expr.setStaticBinding(name, value)
                }

                if (expr.canBeResolvedStatically()) {
                    try {
                        val value = expr.evaluate(config)
                        addSubtree(builder, value)
                    } catch (ex: Exception) {
                        if (onlyChecking) {
                            val message = ex.message ?: ""
                            // An unknown function is always a static error...
                            if (message.contains("Cannot find a") && message.contains("argument function named")) {
                                throw XProcError.xsXPathStaticError(message).exception(ex)
                            }
                            static = false
                            builder.addText("{${avt.value[index]}}")
                        } else {
                            when (ex) {
                                is XProcException -> throw ex
                                is SaxonApiException -> {
                                    val message = ex.message ?: ""
                                    if (message.contains("Cannot find a") && message.contains("function named")) {
                                        throw XProcError.xsXPathStaticError(message).exception(ex)
                                    }
                                    throw XProcError.xdValueTemplateError(ex.message ?: "").exception(ex)
                                }
                                else -> {
                                    throw XProcError.xdInvalidAvtResult(avt.value[index]).exception(ex)
                                }
                            }
                        }
                    }
                } else {
                    if (onlyChecking) {
                        builder.addText(checkValueTemplate(expr, avt.value[index]))
                    } else {
                        expr.contextItem = contextItem
                        for ((name, value) in variableBindings) {
                            expr.setBinding(name, value)
                        }
                        val value = expr.evaluate(config)
                        addSubtree(builder, value)
                    }
                }
            }
        }
    }

    private fun addSubtree(builder: SaxonTreeBuilder, value: XdmValue) {
        if (xmlMediaType) {
            builder.addSubtree(value)
        } else {
            if (value is XdmNode && value.nodeKind == XdmNodeKind.ATTRIBUTE) {
                builder.addText(value.underlyingNode.stringValue)
                return
            }

            val baos = ByteArrayOutputStream()
            val serializer = originalNode.processor.newSerializer(baos)
            serializer.setOutputProperty(Ns.byteOrderMark, "false")
            serializer.setOutputProperty(Ns.method, "text")
            serializer.setOutputProperty(Ns.encoding, "UTF-8")
            serializer.setOutputProperty(Ns.omitXmlDeclaration, "true")
            serializer.serializeXdmValue(value)
            val str = baos.toString(StandardCharsets.UTF_8)
            builder.addText(str)
        }
    }

    private fun checkValueTemplate(expr: XProcExpression, value: String, sb: StringBuilder) {
        static = false
        sb.append("{").append(value).append("}")
        usesContext = usesContext || expr.contextRef

        usesVariables.addAll(expr.variableRefs)
        usesFunctions.addAll(expr.functionRefs)
    }

    private fun checkValueTemplate(expr: XProcExpression, value: String): String {
        static = false
        usesContext = usesContext || expr.contextRef
        usesVariables.addAll(expr.variableRefs)
        usesFunctions.addAll(expr.functionRefs)
        return "{${value}}"
    }

}