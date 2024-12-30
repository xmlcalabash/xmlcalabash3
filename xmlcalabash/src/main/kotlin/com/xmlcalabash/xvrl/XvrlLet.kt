package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlLet private constructor(stepConfiguration: XProcStepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, name: QName, value: String, attr: Map<QName,String> = emptyMap()): XvrlLet {
            val let = XvrlLet(stepConfig)
            let.setAttributes(attr)
            let.setAttribute(Ns.name, "${name}")
            let.setAttribute(Ns.value, value)
            return let
        }

        fun newInstance(stepConfig: XProcStepConfiguration, name: QName, node: XdmNode, attr: Map<QName,String> = emptyMap()): XvrlLet {
            val let = XvrlLet(stepConfig)
            let.withNode(node)
            let.setAttributes(attr)
            let.setAttribute(Ns.name, "${name}")
            return let
        }

        fun newInstance(stepConfig: XProcStepConfiguration, name: QName, nodes: List<XdmNode>, attr: Map<QName,String> = emptyMap()): XvrlLet {
            val let = XvrlLet(stepConfig)
            let.withNodes(nodes)
            let.setAttributes(attr)
            let.setAttribute(Ns.name, "${name}")
            return let
        }
    }

    val name: QName
        get() {
            return stepConfig.parseQName(attributes[Ns.name]!!)
        }

    val value: String?
        get() = attributes[Ns.value]

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.let, stepConfig.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }
}