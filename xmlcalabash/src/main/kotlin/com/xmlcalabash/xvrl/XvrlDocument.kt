package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class XvrlDocument private constructor(stepConfiguration: XProcStepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, href: URI?, attr: Map<QName,String?> = emptyMap()): XvrlDocument {
            val document = XvrlDocument(stepConfig)
            document.setAttributes(attr)
            href?.let { document.setAttribute(Ns.href, it.toString()) }
            return document
        }

        fun newInstance(stepConfig: XProcStepConfiguration, href: URI?, node: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlDocument {
            val document = XvrlDocument(stepConfig)
            document.withNode(node)
            document.setAttributes(attr)
            href?.let { document.setAttribute(Ns.href, it.toString()) }
            return document
        }

        fun newInstance(stepConfig: XProcStepConfiguration, href: URI?, text: String, attr: Map<QName,String?> = emptyMap()): XvrlDocument {
            val document = XvrlDocument(stepConfig)
            document.withText(text)
            document.setAttributes(attr)
            href?.let { document.setAttribute(Ns.href, it.toString()) }
            return document
        }

        fun newInstance(stepConfig: XProcStepConfiguration, href: URI?, nodes: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlDocument {
            val document = XvrlDocument(stepConfig)
            document.withNodes(nodes)
            document.setAttributes(attr)
            href?.let { document.setAttribute(Ns.href, it.toString()) }
            return document
        }
    }

    val href: URI?
        get() = attributes[Ns.href]?.let { URI.create(it) }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.document, stepConfig.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }
}