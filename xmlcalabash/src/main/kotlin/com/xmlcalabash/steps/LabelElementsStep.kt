package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.util.S9Api
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.AttributeInfo
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.FingerprintedQName
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.type.BuiltInAtomicType

open class LabelElementsStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    var matchPattern = "*"
    lateinit var attribute: QName
    lateinit var label: String
    var replace = true
    var index = 0

    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        matchPattern = stringBinding(Ns.match)!!
        attribute = qnameBinding(Ns.attribute)!!
        label = stringBinding(Ns.label)!!
        replace = booleanBinding(Ns.replace)!!

        _matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document.value as XdmNode, matchPattern)

        val doc = matcher.result
        val result = document.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun endDocument(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        index++

        if (node.getAttributeValue(attribute) != null && !replace) {
            matcher.addStartElement(node, attributes)
            return true
        }

        val nsMap = mutableMapOf<String, NamespaceUri>()
        val uriMap = mutableMapOf<NamespaceUri, MutableList<String>>()
        for (binding in node.underlyingNode.allNamespaces) {
            nsMap[binding.prefix] = binding.namespaceUri
            val prefixList = uriMap[binding.namespaceUri] ?: mutableListOf()
            prefixList.add(binding.prefix)
            uriMap[binding.namespaceUri] = prefixList
        }

        var attributes: AttributeMap = EmptyAttributeMap.getInstance()
        var namespaces: NamespaceMap = node.underlyingNode.allNamespaces
        for (ainfo in node.underlyingNode.attributes().asList()) {
            if (ainfo.nodeName.localPart != attribute.localName
                || ainfo.nodeName.namespaceUri != attribute.namespaceUri) {
                attributes = attributes.put(ainfo)
            }
        }

        var addAttribute = attribute
        if (attribute.prefix != "") {
            if (uriMap.containsKey(attribute.namespaceUri)) {
                var addPrefix = if (nsMap.containsKey(attribute.prefix)) {
                    uriMap[attribute.namespaceUri]!!.first()
                } else {
                    attribute.prefix
                }
                nsMap[addPrefix] = attribute.namespaceUri
                namespaces = namespaces.put(addPrefix, attribute.namespaceUri)
                addAttribute = QName(attribute.namespaceUri, "${addPrefix}:${attribute.localName}")
            } else {
                val addPrefix = if (nsMap.containsKey(attribute.prefix)) {
                    S9Api.uniquePrefix(nsMap.keys)
                } else {
                    attribute.prefix
                }
                nsMap[addPrefix] = attribute.namespaceUri
                namespaces = namespaces.put(addPrefix, attribute.namespaceUri)
                addAttribute = QName(attribute.namespaceUri, "${addPrefix}:${attribute.localName}")
            }
        }

        val p_prefix = if ("p" in nsMap) {
            S9Api.uniquePrefix(nsMap.keys)
        } else {
            "p"
        }
        nsMap[p_prefix] = NsP.namespace

        val varname = QName(NsP.namespace, "${p_prefix}:index")

        val compiler = stepConfig.newXPathCompiler()
        for ((prefix, uri) in nsMap) {
            compiler.declareNamespace(prefix, uri.toString())
        }
        compiler.declareVariable(varname)
        val selector = compiler.compile(label).load()
        selector.resourceResolver = stepConfig.environment.documentManager
        selector.setVariable(varname, XdmAtomicValue(index))
        selector.contextItem = node
        val result = selector.evaluate()

        val fqName = FingerprintedQName(addAttribute.prefix, addAttribute.namespaceUri, addAttribute.localName)
        attributes = attributes.put(AttributeInfo(fqName, BuiltInAtomicType.UNTYPED_ATOMIC, result.underlyingValue.stringValue, null, ReceiverOption.NONE))

        matcher.addStartElement(node.nodeName, attributes, namespaces)
        return true
    }

    override fun attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): AttributeMap? {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun endElement(node: XdmNode) {
        matcher.addEndElement()
    }

    override fun text(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun comment(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun pi(node: XdmNode) {
        throw stepConfig.exception(XProcError.xcInvalidSelection(matchPattern))
    }

    override fun toString(): String = "p:label-elements"
}