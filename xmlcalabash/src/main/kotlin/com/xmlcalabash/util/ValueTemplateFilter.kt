package com.xmlcalabash.util

import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.LazyValue
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

interface ValueTemplateFilter {
    fun expandStaticValueTemplates(initialExpand: Boolean, staticBindings: Map<QName, XProcExpression>): XdmNode
    fun expandValueTemplates(contextItem: XProcDocument?, bindings: Map<QName, LazyValue>): XdmNode
    fun containsMarkup(): Boolean
    fun getNode(): XdmNode
    fun isStatic(): Boolean
    fun usesContext(): Boolean
    fun usesVariables(): Set<QName>
    fun usesFunctions(): Set<Pair<QName,Int>>
}