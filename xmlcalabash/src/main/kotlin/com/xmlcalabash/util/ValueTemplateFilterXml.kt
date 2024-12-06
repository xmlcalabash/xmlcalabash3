package com.xmlcalabash.util

import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.runtime.LazyValue
import net.sf.saxon.s9api.*
import java.net.URI
import java.util.*

class ValueTemplateFilterXml(val stepConfig: StepConfiguration, val originalNode: XdmNode, val baseUri: URI): ValueTemplateFilter {
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

    private var contextItem: Any? = null

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

    override fun expandStaticValueTemplates(initialExpand: Boolean, staticBindings: Map<QName, XProcExpression>): XdmNode {
        contextItem = null
        this.initialExpand = initialExpand
        expandText.clear()
        expandText.push(initialExpand)

        staticVariableBindings.clear()
        staticVariableBindings.putAll(staticBindings)

        static = true
        onlyChecking = true
        var builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(baseUri)
        filterValueTemplates(builder, originalNode)
        builder.endDocument()

        expandText.pop()

        if (static) {
            val xml = builder.result
            builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(baseUri)
            removeInlineExpandText(builder, xml)
            builder.endDocument()
            xmlNode = builder.result
        }

        return xmlNode
    }

    override fun expandValueTemplates(contextItem: XProcDocument?, bindings: Map<QName, LazyValue>): XdmNode {
        return expandValueTemplatesAny(contextItem, bindings)
    }

    private fun expandValueTemplatesAny(contextItem: XProcDocument?, bindings: Map<QName, LazyValue>): XdmNode {
        this.contextItem = contextItem

        expandText.clear()
        expandText.push(initialExpand)

        variableBindings.clear()
        for ((name, doc) in bindings) {
            variableBindings[name] = doc.value
        }

        onlyChecking = false
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(originalNode.baseURI)
        filterValueTemplates(builder, originalNode)
        builder.endDocument()

        expandText.pop()

        return builder.result
    }

    private fun filterValueTemplates(builder: SaxonTreeBuilder, node: XdmNode) {
        when (node.nodeKind) {
            XdmNodeKind.DOCUMENT -> node.axisIterator(Axis.CHILD).forEach { filterValueTemplates(builder, it) }
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
                            considerValueTemplates(node, attr.stringValue)
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

                builder.addStartElement(node, stepConfig.attributeMap(attrMap))
                node.axisIterator(Axis.CHILD).forEach { filterValueTemplates(builder, it) }
                builder.addEndElement()

                expandText.pop()
            }
            XdmNodeKind.TEXT -> {
                if (expandText.peek()) {
                    considerValueTemplates(builder, node.parent, node.stringValue)
                } else {
                    builder.addSubtree(node)
                }
            }
            else -> builder.addSubtree(node)
        }
    }

    private fun removeInlineExpandText(builder: SaxonTreeBuilder, node: XdmNode) {
        when (node.nodeKind) {
            XdmNodeKind.DOCUMENT -> node.axisIterator(Axis.CHILD).forEach { removeInlineExpandText(builder, it) }
            XdmNodeKind.ELEMENT -> {
                val attrMap = mutableMapOf<QName, String?>()
                node.axisIterator(Axis.ATTRIBUTE).forEach { attr ->
                    if (attr.nodeName != NsP.inlineExpandText) {
                        attrMap[attr.nodeName] = attr.stringValue
                    }
                }
                builder.addStartElement(node, stepConfig.attributeMap(attrMap))
                node.axisIterator(Axis.CHILD).forEach { removeInlineExpandText(builder, it) }
                builder.addEndElement()
            }
            else -> builder.addSubtree(node)
        }
    }

    private fun considerValueTemplates(context: XdmNode, text: String): String {
        val avt = ValueTemplateParser.parse(text)

        if (avt.value.size == 1) {
            // There are no value templates in here.
            return avt.value[0]
        }

        val avtConfig = stepConfig.copy()
        avtConfig.updateWith(context)

        val sb = StringBuilder()
        for (index in avt.value.indices) {
            if (index % 2 == 0) {
                sb.append(avt.value[index])
            } else {
                val expr = XProcExpression.select(avtConfig, avt.value[index])
                for ((name, value) in staticVariableBindings) {
                    expr.setStaticBinding(name, value)
                }

                if (expr.canBeResolvedStatically()) {
                    val value = expr.evaluate()
                    sb.append(value.underlyingValue.stringValue)
                } else {
                    if (onlyChecking) {
                        checkValueTemplate(expr, avt.value[index], sb)
                    } else {
                        expr.contextItem = contextItem
                        for ((name, value) in variableBindings) {
                            expr.setBinding(name, value)
                        }
                        val value = expr.evaluate()
                        sb.append(value.underlyingValue.stringValue)
                    }
                }
            }
        }

        return sb.toString()
    }

    private fun considerValueTemplates(builder: SaxonTreeBuilder, context: XdmNode, text: String) {
        val avt = ValueTemplateParser.parse(text)

        val avtConfig = stepConfig.copy()
        avtConfig.updateWith(context)

        if (avt.value.size == 1) {
            // There are no value templates in here.
            builder.addText(avt.value[0])
            return
        }

        for (index in avt.value.indices) {
            if (index % 2 == 0) {
                builder.addText(avt.value[index])
            } else {
                val expr = XProcExpression.select(avtConfig, avt.value[index])
                for ((name, value) in staticVariableBindings) {
                    expr.setStaticBinding(name, value)
                }

                if (expr.canBeResolvedStatically()) {
                    try {
                        val value = expr.evaluate()
                        builder.addSubtree(value)
                    } catch (ex: Exception) {
                        if (onlyChecking) {
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
                        val value = expr.evaluate()
                        builder.addSubtree(value)
                    }
                }
            }
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