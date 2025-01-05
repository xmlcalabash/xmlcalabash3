package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.parameters.StepParameters
import net.sf.saxon.s9api.*

open class JsonMergeStep(): AbstractAtomicStep() {
    var duplicates = "use-first"
    var key = ""
    var index = 0

    override fun run() {
        super.run()

        val inputs = queues["source"]!!
        duplicates = stringBinding(Ns.duplicates) ?: "use-first"
        key = stringBinding(Ns.key) ?: "concat(\"_\",\$p:index)"

        var value = XdmMap()
        for (input in inputs) {
            index++
            when (input.value) {
                is XdmMap -> value = addMap(value, input.value as XdmMap)
                is XdmAtomicValue -> value = addValue(value, input.value)
                is XdmArray -> value = addValue(value, input.value)
                is XdmNode -> value = addValue(value, input.value)
                else -> throw stepConfig.exception(XProcError.xcUnsupportedForJsonMerge())
            }
        }

        receiver.output("result", XProcDocument.ofJson(value, stepConfig))
    }

    private fun addMap(value: XdmMap, map: XdmMap): XdmMap {
        var newValue = value
        for (key in map.keySet()) {
            if (newValue.containsKey(key)) {
                when (duplicates) {
                    "reject" -> throw stepConfig.exception(XProcError.xcDuplicateKeyInJsonMerge(key))
                    "use-first" -> Unit
                    "use-last" -> newValue = newValue.put(key, map.get(key))
                    "use-any" -> Unit // first will do
                    "combine" -> {
                        var mapValue = newValue.get(key)
                        mapValue = mapValue.append(map.get(key))
                        newValue = newValue.put(key, mapValue)
                    }
                }
            } else {
                newValue = newValue.put(key, map.get(key))
            }
        }
        return newValue
    }

    private fun addValue(value: XdmMap, item: XdmValue): XdmMap {
        var newValue = value

        val nsbindings = establishNamespaceBinding("p", NsP.namespace, stepConfig.inscopeNamespaces)
        val indexName = QName(nsbindings.first, NsP.namespace.toString(), "index")

        val compiler = stepConfig.newXPathCompiler()
        for ((prefix, uri) in nsbindings.second) {
            compiler.declareNamespace(prefix, uri.toString())
        }
        compiler.declareVariable(indexName)
        val selector = compiler.compile(key).load()
        selector.resourceResolver = stepConfig.environment.documentManager
        selector.setVariable(indexName, XdmAtomicValue(index))
        selector.contextItem = item.itemAt(0) // ???
        val result = selector.evaluate()
        if (result.size() != 1) {
            throw stepConfig.exception(XProcError.xcInvalidKeyForJsonMerge())
        }

        val key = when (result) {
            is XdmAtomicValue -> result
            is XdmMap, is XdmArray, is XdmFunctionItem -> throw stepConfig.exception(XProcError.xcInvalidKeyForJsonMerge())
            else -> XdmAtomicValue(result.underlyingValue.stringValue)
        }

        if (newValue.containsKey(key)) {
            when (duplicates) {
                "reject" -> throw stepConfig.exception(XProcError.xcDuplicateKeyInJsonMerge(key))
                "use-first" -> Unit
                "use-last" -> newValue = newValue.put(key, item)
                "use-any" -> Unit // first will do
                "combine" -> {
                    var mapValue = newValue.get(key)
                    mapValue = mapValue.append(item)
                    newValue = newValue.put(key, mapValue)
                }
            }
        } else {
            newValue = newValue.put(key, item)
        }

        return newValue
    }

    override fun toString(): String = "p:json-merge"
}