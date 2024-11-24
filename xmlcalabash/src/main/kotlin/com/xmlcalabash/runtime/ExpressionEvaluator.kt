package com.xmlcalabash.runtime

import com.xmlcalabash.datamodel.XProcExpression
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmItem
import net.sf.saxon.s9api.XdmValue

class ExpressionEvaluator(val processor: Processor, val select: String) {
    private val namespaceBindings = mutableMapOf<String, NamespaceUri>()
    private val variableBindings = mutableMapOf<QName, XdmValue>()
    private var contextItem: XdmItem? = null

    fun setNamespaces(namespaces: Map<String, NamespaceUri>) {
        namespaceBindings.clear()
        namespaceBindings.putAll(namespaces)
    }

    fun setBindings(bindings: Map<QName, XdmValue>) {
        variableBindings.clear()
        variableBindings.putAll(bindings)
    }

    fun setExpressionBindings(bindings: Map<QName, LazyValue>) {
        variableBindings.clear()
        for ((name, binding) in bindings) {
            variableBindings[name] = binding.value;
        }
    }

    fun setContext(item: XdmItem) {
        contextItem = item
    }

    fun evaluate(): XdmValue {
        val compiler = processor.newXPathCompiler()
        for ((prefix, uri) in namespaceBindings) {
            compiler.declareNamespace(prefix, uri.toString())
        }
        for ((name, _) in variableBindings) {
            compiler.declareVariable(name)
        }
        val exec = compiler.compile(select)
        val select = exec.load()
        for ((name, value) in variableBindings) {
            select.setVariable(name, value)
        }
        if (contextItem != null) {
            select.contextItem = contextItem
        }
        return select.evaluate()
    }
}