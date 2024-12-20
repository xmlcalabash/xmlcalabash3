package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.runtime.parameters.StepParameters
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

open class UuidStep(): AbstractAtomicStep(), ProcessMatchingNodes {
    lateinit var document: XProcDocument
    lateinit var matcher: ProcessMatch

    var matchPattern = "/*"
    var version = 0
    var uuid = ""

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        matchPattern = stringBinding(Ns.match)!!
        version = integerBinding(Ns.version) ?: 4

        if (version == 4) {
            uuid = java.util.UUID.randomUUID().toString()
        } else {
            throw stepConfig.exception(XProcError.xcUnsupportedUuidVersion(version))
        }

        matcher = ProcessMatch(stepConfig, this, valueBinding(Ns.match).context.inscopeNamespaces)
        matcher.process(document.value as XdmNode, matchPattern)

        val doc = matcher.result
        val result = document.with(doc)

        receiver.output("result", result)
    }

    override fun startDocument(node: XdmNode): Boolean {
        matcher.addText(uuid)
        return false
    }

    override fun endDocument(node: XdmNode) {
        matcher.endDocument()
    }

    override fun startElement(node: XdmNode, attributes: AttributeMap): Boolean {
        matcher.addText(uuid)
        return false
    }

    override fun attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): AttributeMap? {
        val amap = mutableMapOf<QName, String?>()
        amap.putAll(stepConfig.attributeMap(nonMatchingAttributes))

        for (attr in matchingAttributes.asList()) {
            val qname = QName(attr.nodeName.prefix, attr.nodeName.namespaceUri.toString(), attr.nodeName.localPart)
            amap[qname] = uuid
        }

        return stepConfig.attributeMap(amap)
    }

    override fun endElement(node: XdmNode) {
        // nop
    }

    override fun text(node: XdmNode) {
        matcher.addText(uuid)
    }

    override fun comment(node: XdmNode) {
        matcher.addText(uuid)
    }

    override fun pi(node: XdmNode) {
        matcher.addText(uuid)
    }

    override fun toString(): String = "p:uuid"
}