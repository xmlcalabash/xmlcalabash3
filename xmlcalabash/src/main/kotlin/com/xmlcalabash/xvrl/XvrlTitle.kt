package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlTitle private constructor(stepConfiguration: XProcStepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, node: XdmNode, attr: Map<QName,String> = emptyMap()): XvrlTitle {
            val title = XvrlTitle(stepConfig)
            title.withNode(node)
            title.setAttributes(attr)
            return title
        }

        fun newInstance(stepConfig: XProcStepConfiguration, text: String, attr: Map<QName,String> = emptyMap()): XvrlTitle {
            val title = XvrlTitle(stepConfig)
            title.withText(text)
            title.setAttributes(attr)
            return title
        }

        fun newInstance(stepConfig: XProcStepConfiguration, nodes: List<XdmNode>, attr: Map<QName,String> = emptyMap()): XvrlTitle {
            val title = XvrlTitle(stepConfig)
            title.withNodes(nodes)
            title.setAttributes(attr)
            return title
        }
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.title, stepConfig.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }
}