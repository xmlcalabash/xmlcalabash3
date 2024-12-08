package com.xmlcalabash.util

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.runtime.LazyValue
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import java.net.URI

class ValueTemplateFilterNone(val encodedNode: XdmNode, val baseUri: URI): ValueTemplateFilter {
    override fun containsMarkup(config: XProcStepConfiguration): Boolean {
        return false
    }

    override fun getNode(): XdmNode {
        return encodedNode
    }

    override fun isStatic(): Boolean {
        return true
    }

    override fun usesContext(): Boolean {
        return false
    }

    override fun usesVariables(): Set<QName> {
        return setOf()
    }

    override fun usesFunctions(): Set<Pair<QName,Int>> {
        return setOf()
    }

    override fun expandStaticValueTemplates(config: XProcStepConfiguration, initialExpand: Boolean, staticBindings: Map<QName,XProcExpression>): XdmNode {
        return encodedNode
    }

    override fun expandValueTemplates(config: XProcStepConfiguration, contextItem: XProcDocument?, bindings: Map<QName, LazyValue>): XdmNode {
        return encodedNode
    }
}