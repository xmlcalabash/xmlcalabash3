package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlCategory private constructor(stepConfig: XProcStepConfiguration): XvrlContainer(stepConfig) {
    companion object {
        private val _vocabulary = QName("vocabulary")

        fun newInstance(stepConfig: XProcStepConfiguration, text: String? = null, vocabulary: String? = null, attr: Map<QName,String> = emptyMap()): XvrlCategory {
            val cat = XvrlCategory(stepConfig)
            text?.let { cat.withText(it) }
            cat.setAttributes(attr)
            vocabulary?.let { cat.setAttribute(_vocabulary, it)}
            return cat
        }

        fun newInstance(stepConfig: XProcStepConfiguration, node: XdmNode, vocabulary: String? = null, attr: Map<QName,String> = emptyMap()): XvrlCategory {
            val cat = XvrlCategory(stepConfig)
            cat.withNode(node)
            cat.setAttributes(attr)
            vocabulary?.let { cat.setAttribute(_vocabulary, it)}
            return cat
        }

        fun newInstance(stepConfig: XProcStepConfiguration, nodes: List<XdmNode>, vocabulary: String? = null, attr: Map<QName,String> = emptyMap()): XvrlCategory {
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
        builder.addStartElement(NsXvrl.category, stepConfig.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }

}