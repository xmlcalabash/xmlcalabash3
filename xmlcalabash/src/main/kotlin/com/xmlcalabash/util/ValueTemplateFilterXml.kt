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
import net.sf.saxon.trans.UncheckedXPathException
import net.sf.saxon.trans.XPathException
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

                val nodes = mutableListOf<Pair<XdmValue?,String?>>()
                node.axisIterator(Axis.ATTRIBUTE).forEach { attr ->
                    if (attr.nodeName != inlineAttribute && attr.nodeName != NsP.inlineExpandText) {
                        val value = if (expand) {
                            considerValueTemplates(config, node, attr.stringValue)
                        } else {
                            attr.stringValue
                        }
                        nodes.add(Pair(attr, value))
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

                for (child in node.axisIterator(Axis.CHILD)) {
                    nodes.addAll(filterValueTemplateNodes(config, child))
                }

                var done = nodes.isEmpty()
                while (!done) {
                    val childPair = nodes.first()
                    if (childPair.first is XdmNode && (childPair.first as XdmNode).nodeKind == XdmNodeKind.ATTRIBUTE) {
                        val name = (childPair.first as XdmNode).nodeName
                        val value = childPair.second ?: (childPair.first as XdmNode).underlyingValue.stringValue
                        attrMap[name] = value
                        nodes.removeFirst()
                        done = nodes.isEmpty()
                    } else {
                        done = true
                    }
                }

                builder.addStartElement(node, config.attributeMap(attrMap))
                for (childPair in nodes) {
                    if (childPair.first != null) {
                        builder.addSubtree(childPair.first!!)
                    } else {
                        builder.addText(childPair.second!!)
                    }
                }
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

    private fun filterValueTemplateNodes(config: XProcStepConfiguration, node: XdmNode): List<Pair<XdmValue?,String?>> {
        val nodes = mutableListOf<Pair<XdmValue?,String?>>()
        when (node.nodeKind) {
            XdmNodeKind.ELEMENT -> {
                val builder = SaxonTreeBuilder(config)
                builder.startDocument(node.baseURI)
                filterValueTemplates(config, builder, node)
                builder.endDocument()
                val node = S9Api.documentElement(builder.result)
                nodes.add(Pair(node, null))
            }
            XdmNodeKind.TEXT -> {
                if (expandText.peek()) {
                    nodes.addAll(considerValueTemplateNodes(config, node.parent, node.stringValue))
                    considerValueTemplates(config, node.parent, node.stringValue)
                } else {
                    nodes.add(Pair(node, null))
                }
            }
            else -> nodes.add(Pair(node, null))
        }
        return nodes
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
                    try {
                        val value = expr.evaluate(config)
                        sb.append(value.underlyingValue.stringValue)
                    } catch (ex: Exception) {
                        when (ex) {
                            is XProcException -> throw ex
                            is UncheckedXPathException -> {
                                throw XProcError.xdInvalidAvtResult(text).exception(ex)
                            }
                        }
                        throw XProcError.xdValueTemplateError(ex.message ?: "(no message)").exception(ex)
                    }
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
        val nodes = considerValueTemplateNodes(config, context, text)
        for (pair in nodes) {
            if (pair.first != null) {
                val value = pair.first!!
                if (value is XdmNode || value is XdmAtomicValue) {
                    addSubtree(builder, pair.first!!)
                } else {
                    throw XProcError.xdInvalidAvtResult(text).exception()
                }
            } else {
                builder.addText(pair.second!!)
            }
        }
    }

    private fun considerValueTemplateNodes(config: XProcStepConfiguration, context: XdmNode, text: String): List<Pair<XdmValue?,String?>> {
        val nodes = mutableListOf<Pair<XdmValue?,String?>>()
        val avt = ValueTemplateParser.parse(config, text)

        if (avt.value.size == 1) {
            // There are no value templates in here.
            if (avt.value[0] != "") {
                nodes.add(Pair(null, avt.value[0]))
            }
            return nodes
        }

        for (index in avt.value.indices) {
            if (index % 2 == 0) {
                if (avt.value[index] != "") {
                    nodes.add(Pair(null, avt.value[index]))
                }
            } else {
                val expr = XProcExpression.select(config, avt.value[index])
                for ((name, value) in staticVariableBindings) {
                    expr.setStaticBinding(name, value)
                }

                if (expr.canBeResolvedStatically()) {
                    try {
                        val value = expr.evaluate(config)
                        nodes.add(Pair(value, null))
                    } catch (ex: Exception) {
                        if (onlyChecking) {
                            val message = ex.message ?: ""
                            // An unknown function is always a static error...
                            if (message.contains("Cannot find a") && message.contains("argument function named")) {
                                throw XProcError.xsXPathStaticError(message).exception(ex)
                            }
                            static = false
                            nodes.add(Pair(null, "{${avt.value[index]}}"))
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
                        nodes.add(Pair(null, checkValueTemplate(expr, avt.value[index])))
                    } else {
                        expr.contextItem = contextItem
                        for ((name, value) in variableBindings) {
                            expr.setBinding(name, value)
                        }
                        val value = expr.evaluate(config)
                        nodes.add(Pair(value, null))
                    }
                }
            }
        }
        return nodes
    }

    private fun addSubtree(builder: SaxonTreeBuilder, value: XdmValue) {
        try {
            if (xmlMediaType) {
                builder.addSubtree(value)
            } else {
                val baos = ByteArrayOutputStream()
                val serializer = originalNode.processor.newSerializer(baos)
                serializer.setOutputProperty(Ns.byteOrderMark, "false")
                serializer.setOutputProperty(Ns.encoding, "UTF-8")
                serializer.setOutputProperty(Ns.mediaType, "text/plain")
                serializer.setOutputProperty(Ns.method, "text")
                serializer.setOutputProperty(Ns.normalizationForm, "NFC")
                serializer.serializeXdmValue(value)
                val str = baos.toString(StandardCharsets.UTF_8)
                builder.addText(str)
            }
        } catch (ex: Exception) {
            if (ex is SaxonApiException || ex is XPathException) {
                val message = ex.message ?: ""
                if (message.contains("Cannot process free-standing attribute")) {
                    val pos = message.indexOf('(')
                    val name = message.substring(pos+1, message.length - 1)
                    throw XProcError.xdTvtCannotSerializeAttributes(name).exception()
                }
            }
            throw ex
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