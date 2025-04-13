package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode

abstract class XvrlContainer(stepConfiguration: StepConfiguration): XvrlElement(stepConfiguration) {
    private var _content: XdmNode? = null

    val content: XdmNode?
        get() = _content

    protected fun withNode(node: XdmNode) {
        _content = node
    }

    protected fun withNodes(nodes: List<XdmNode>) {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        for (node in nodes) {
            builder.addSubtree(node)
        }
        builder.endDocument()
        _content = builder.result

    }

    protected fun withText(text: String) {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addText(text)
        builder.endDocument()
        _content = builder.result
    }

    protected fun serializeContent(builder: SaxonTreeBuilder) {
        content?.let { builder.addSubtree(it) }
    }
}