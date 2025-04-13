package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlSupplemental private constructor(stepConfiguration: StepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: StepConfiguration, node: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlSupplemental {
            val supplemental = XvrlSupplemental(stepConfig)
            supplemental.withNode(node)
            supplemental.setAttributes(attr)
            return supplemental
        }

        fun newInstance(stepConfig: StepConfiguration, text: String?, attr: Map<QName,String?> = emptyMap()): XvrlSupplemental {
            val supplemental = XvrlSupplemental(stepConfig)
            text?.let { supplemental.withText(it) }
            supplemental.setAttributes(attr)
            return supplemental
        }

        fun newInstance(stepConfig: StepConfiguration, nodes: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlSupplemental {
            val supplemental = XvrlSupplemental(stepConfig)
            supplemental.withNodes(nodes)
            supplemental.setAttributes(attr)
            return supplemental
        }
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.supplemental, stepConfig.typeUtils.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }
}