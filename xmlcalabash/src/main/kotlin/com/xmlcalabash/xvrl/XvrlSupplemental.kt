package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlSupplemental private constructor(stepConfiguration: XProcStepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, node: XdmNode, attr: Map<QName,String> = emptyMap()): XvrlSupplemental {
            val supplemental = XvrlSupplemental(stepConfig)
            supplemental.withNode(node)
            supplemental.setAttributes(attr)
            return supplemental
        }

        fun newInstance(stepConfig: XProcStepConfiguration, text: String?, attr: Map<QName,String> = emptyMap()): XvrlSupplemental {
            val supplemental = XvrlSupplemental(stepConfig)
            text?.let { supplemental.withText(it) }
            supplemental.setAttributes(attr)
            return supplemental
        }

        fun newInstance(stepConfig: XProcStepConfiguration, nodes: List<XdmNode>, attr: Map<QName,String> = emptyMap()): XvrlSupplemental {
            val supplemental = XvrlSupplemental(stepConfig)
            supplemental.withNodes(nodes)
            supplemental.setAttributes(attr)
            return supplemental
        }
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.supplemental, stepConfig.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }
}