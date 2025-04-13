package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlSummary private constructor(stepConfiguration: StepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: StepConfiguration, node: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlSummary {
            val summary = XvrlSummary(stepConfig)
            summary.withNode(node)
            summary.setAttributes(attr)
            return summary
        }

        fun newInstance(stepConfig: StepConfiguration, text: String, attr: Map<QName,String?> = emptyMap()): XvrlSummary {
            val summary = XvrlSummary(stepConfig)
            summary.withText(text)
            summary.setAttributes(attr)
            return summary
        }

        fun newInstance(stepConfig: StepConfiguration, nodes: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlSummary {
            val summary = XvrlSummary(stepConfig)
            summary.withNodes(nodes)
            summary.setAttributes(attr)
            return summary
        }
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.summary, stepConfig.typeUtils.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }
}