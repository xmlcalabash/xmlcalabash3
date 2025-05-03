package com.xmlcalabash.datamodel

import net.sf.saxon.Configuration
import net.sf.saxon.functions.ExecutableFunctionLibrary
import net.sf.saxon.functions.FunctionLibrary
import net.sf.saxon.query.XQueryFunctionLibrary
import net.sf.saxon.s9api.QName

class XProcFunctionLibrary(val exposedNames: Map<QName, Int>, val library: FunctionLibrary): FunctionLibrary by library {
    override fun setConfiguration(config: Configuration?) {
        library.setConfiguration(config)
    }

    fun suppressDuplicates(seen: Map<QName, Int>, config: Configuration): FunctionLibrary {
        for ((key, arity) in exposedNames) {
            if (seen[key] == arity) {
                when (library) {
                    is XQueryFunctionLibrary -> return suppressedXQuery(seen, library)
                    is ExecutableFunctionLibrary -> return suppressedExecutable(seen, library, config)
                    else -> throw IllegalStateException("Unknown function library type ${library.javaClass}")
                }
            }
        }
        return library
    }

    private fun suppressedXQuery(seen: Map<QName, Int>, library: XQueryFunctionLibrary): FunctionLibrary {
        val functionLibrary = XQueryFunctionLibrary(library.configuration)
        for (function in library.functionDefinitions) {
            val name = if (function.functionName.prefix == "") {
                QName(function.functionName.namespaceUri, function.functionName.localPart)
            } else {
                QName(function.functionName.namespaceUri, "${function.functionName.prefix}:${function.functionName.localPart}")
            }
            if (seen[name] != function.numberOfParameters) {
                functionLibrary.declareFunction(function)
            }
        }
        return functionLibrary
    }

    private fun suppressedExecutable(seen: Map<QName, Int>, library: ExecutableFunctionLibrary, config: Configuration): FunctionLibrary {
        val functionLibrary = ExecutableFunctionLibrary(config)
        for (function in library.allFunctions) {
            val name = if (function.functionName.prefix == "") {
                QName(function.functionName.namespaceUri, function.functionName.localPart)
            } else {
                QName(function.functionName.namespaceUri, "${function.functionName.prefix}:${function.functionName.localPart}")
            }

            if (seen[name] != function.numberOfParameters) {
                functionLibrary.addFunction(function)
            }
        }
        return functionLibrary
    }
}