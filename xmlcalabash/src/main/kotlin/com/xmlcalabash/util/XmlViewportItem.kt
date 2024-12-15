package com.xmlcalabash.util

import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.XdmNode

class XmlViewportItem(val stepConfig: XProcStepConfiguration, val node: XdmNode) {
    private var _replacement: XdmNode? = null

    val replacement: XdmNode
        get() = _replacement ?: throw RuntimeException("Configuration error: no replacement")

    fun replaceWith(items: List<XdmNode>) {
        if (items.size == 1) {
            _replacement = items[0]
        } else {
            val builder = SaxonTreeBuilder(stepConfig)
            if (items.isEmpty()) {
                builder.startDocument(null)
            } else {
                builder.startDocument(items[0].baseURI)
            }
            for (item in items) {
                builder.addSubtree(item)
            }
            builder.endDocument()
            _replacement = builder.result
        }
    }
}