package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class XvrlContext private constructor(stepConfiguration: StepConfiguration): XvrlContainer(stepConfiguration) {
    var location: XvrlLocation? = null

    companion object {
        fun newInstance(stepConfig: StepConfiguration, text: String?, attr: Map<QName,String?> = emptyMap()): XvrlContext {
            val context = XvrlContext(stepConfig)
            text?.let { context.withText(it) }
            context.setAttributes(attr)
            return context
        }

        fun newInstance(stepConfig: StepConfiguration, node: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlContext {
            val context = XvrlContext(stepConfig)
            context.withNode(node)
            context.setAttributes(attr)
            return context
        }

        fun newInstance(stepConfig: StepConfiguration, nodes: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlContext {
            val context = XvrlContext(stepConfig)
            context.withNodes(nodes)
            context.setAttributes(attr)
            return context
        }
    }

    // ============================================================

    fun location(href: URI? = null): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href)
        return location!!
    }

    fun location(href: URI?, line: Int, column: Int = 0): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href, line, column)
        return location!!
    }

    fun location(href: URI?, offset: Int): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href, offset)
        return location!!
    }

    // ============================================================

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.context, stepConfig.typeUtils.attributeMap(attributes))
        location?.serialize(builder)
        serializeContent(builder)
        builder.addEndElement()
    }
}