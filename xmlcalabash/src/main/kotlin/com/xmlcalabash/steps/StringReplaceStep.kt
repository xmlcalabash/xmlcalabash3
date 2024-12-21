package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ExpressionEvaluator
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

open class StringReplaceStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    var matchPattern = "/*"
    var replace: String = ""

    var _matcher: ProcessMatch? = null
    val matcher: ProcessMatch
        get() = _matcher ?: throw RuntimeException("Configuration error...")

    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        matchPattern = stringBinding(Ns.match)!!
        replace = stringBinding(Ns.replace)!!

        _matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document.value as XdmNode, matchPattern)

        val doc = matcher.result
        val props = DocumentProperties(document.properties)
        val result = document.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        matcher.addText(evaluate(node))
        return false
    }

    override fun endDocument(node: XdmNode) {
        matcher.endDocument()
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        matcher.addText(evaluate(node))
        return false
    }

    override fun attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): AttributeMap? {
        val amap = mutableMapOf<QName, String?>()
        amap.putAll(stepConfig.attributeMap(nonMatchingAttributes))

        for (attr in matchingAttributes.asList()) {
            val qname = QName(attr.nodeName.prefix, attr.nodeName.namespaceUri.toString(), attr.nodeName.localPart)
            var attrNode: XdmNode = node // not an attribute, but also not null
            for (anode in node.axisIterator(Axis.ATTRIBUTE)) {
                if (anode.nodeName == qname) {
                    attrNode = anode
                    break
                }
            }
            amap[qname] = evaluate(attrNode)
        }

        return stepConfig.attributeMap(amap)
    }

    override fun endElement(node: XdmNode) {
        // nop
    }

    override fun text(node: XdmNode) {
        matcher.addText(evaluate(node))
    }

    override fun comment(node: XdmNode) {
        matcher.addText(evaluate(node))
    }

    override fun pi(node: XdmNode) {
        matcher.addText(evaluate(node))
    }

    private fun evaluate(contextItem: XdmNode): String {
        val evaluator = ExpressionEvaluator(stepConfig.processor, replace)
        evaluator.setNamespaces(stepConfig.inscopeNamespaces)
        evaluator.setContext(contextItem)
        return evaluator.evaluate().underlyingValue.stringValue
    }

    override fun toString(): String = "p:string-replace"
}