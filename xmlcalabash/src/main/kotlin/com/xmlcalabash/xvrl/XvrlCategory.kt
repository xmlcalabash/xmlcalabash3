package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlCategory private constructor(stepConfig: StepConfiguration): XvrlContainer(stepConfig) {
    companion object {
        private val _vocabulary = QName("vocabulary")

        fun newInstance(stepConfig: StepConfiguration, text: String? = null, vocabulary: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlCategory {
            val cat = XvrlCategory(stepConfig)
            text?.let { cat.withText(it) }
            cat.setAttributes(attr)
            vocabulary?.let { cat.setAttribute(_vocabulary, it)}
            return cat
        }

        fun newInstance(stepConfig: StepConfiguration, node: XdmNode, vocabulary: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlCategory {
            val cat = XvrlCategory(stepConfig)
            cat.withNode(node)
            cat.setAttributes(attr)
            vocabulary?.let { cat.setAttribute(_vocabulary, it)}
            return cat
        }

        fun newInstance(stepConfig: StepConfiguration, nodes: List<XdmNode>, vocabulary: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlCategory {
            val cat = XvrlCategory(stepConfig)
            cat.withNodes(nodes)
            cat.setAttributes(attr)
            vocabulary?.let { cat.setAttribute(_vocabulary, it)}
            return cat
        }
    }

    val vocabulary: String?
        get() = attributes[_vocabulary]

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.category, stepConfig.typeUtils.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }

}