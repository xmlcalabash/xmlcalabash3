package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlCreator private constructor(stepConfiguration: XProcStepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, name: String, version: String?, attr: Map<QName,String> = emptyMap()): XvrlCreator {
            val creator = XvrlCreator(stepConfig)
            creator.setAttributes(attr)
            creator.setAttribute(Ns.name, name)
            version?.let { creator.setAttribute(Ns.version, it) }
            return creator
        }

        fun newInstance(stepConfig: XProcStepConfiguration, name: String, version: String?, node: XdmNode, attr: Map<QName,String> = emptyMap()): XvrlCreator {
            val creator = XvrlCreator(stepConfig)
            creator.withNode(node)
            creator.setAttributes(attr)
            creator.setAttribute(Ns.name, name)
            version?.let { creator.setAttribute(Ns.version, it) }
            return creator
        }

        fun newInstance(stepConfig: XProcStepConfiguration, name: String, version: String?, text: String, attr: Map<QName,String> = emptyMap()): XvrlCreator {
            val creator = XvrlCreator(stepConfig)
            creator.withText(text)
            creator.setAttributes(attr)
            creator.setAttribute(Ns.name, name)
            version?.let { creator.setAttribute(Ns.version, it) }
            return creator
        }

        fun newInstance(stepConfig: XProcStepConfiguration, name: String, version: String?, nodes: List<XdmNode>, attr: Map<QName,String> = emptyMap()): XvrlCreator {
            val creator = XvrlCreator(stepConfig)
            creator.withNodes(nodes)
            creator.setAttributes(attr)
            creator.setAttribute(Ns.name, name)
            version?.let { creator.setAttribute(Ns.version, it) }
            return creator
        }
    }

    val name: String
        get() = attributes[Ns.name]!!

    val version: String?
        get() = attributes[Ns.version]

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.creator, stepConfig.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }
}