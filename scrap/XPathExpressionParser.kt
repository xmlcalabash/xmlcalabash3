package com.xmlcalabash.parsers

import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.parsers.XPath31.EventHandler
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.config.XProcStepConfiguration
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import java.util.*

class XPathExpressionParser(val stepConfig: XProcStepConfiguration) {
    companion object {
        private val TRACE = false

        // FIXME: what's the right list here?
        val contextDependentFunctions = setOf(
            NsFn.collection, NsFn.baseUri)

        val alwaysDynamicFunctions = setOf(
            NsP.systemProperty, NsP.iterationPosition, NsP.iterationSize, NsP.documentProperties,
            NsP.documentPropertiesDocument, NsP.documentProperty,
            NsFn.currentDate, NsFn.currentDateTime, NsFn.currentTime,
            NsFn.doc, NsFn.docAvailable, NsFn.document, NsFn.unparsedText,
            NsFn.unparsedTextAvailable)
    }

    fun parse(expr: String): XPathExpressionDetails {
        var error: Exception? = null
        val handler = FindRefs(TRACE)
        handler.initialize()

        // Hack. This doesn't trigger a ContextItemExpr
        handler.context = expr.startsWith("/")

        val parser = XPath31(expr, handler)
        try {
            parser.parse_XPath()
        } catch (ex: Exception) {
            error = ex
        }

        val variables = mutableSetOf<QName>()
        for (name in handler.varlist) {
            variables.add(stepConfig.parseQName(name))
        }

        var usesContext = handler.context
        var alwaysDynamic = false
        val functions = mutableSetOf<Pair<QName,Int>>()
        for (name in handler.funclist) {
            val fname = stepConfig.parseQName(name.first)
            val fqname = if (fname.namespaceUri == NamespaceUri.NULL) {
                QName(NsFn.namespace, fname.localName)
            } else {
                fname
            }
            usesContext = usesContext || contextDependentFunctions.contains(fqname)
            alwaysDynamic = alwaysDynamic || alwaysDynamicFunctions.contains(fqname)
            functions.add(Pair(fqname, name.second))
        }

        return XPathExpressionDetails(error, variables, functions, usesContext, alwaysDynamic)
    }

    private enum class XPathState {
        IGNORE, PATHEXPR, RELPATHEXPR, STEPEXPR, AXISSTEP, POSTFIXEXPR, PRIMARYEXPR
    }

    private class FindRefs(val trace: Boolean): EventHandler {
        private var input: String = ""
        private var sawDollar = false
        // Simple switches won't work if they can nest, but I don't think they can...
        private var functionCall = false
        private var functionName = false
        private var functionStack = Stack<Pair<String,Int>>()
        private var quantified = false // I bet this one can nest...
        private var quantvar: MutableSet<String> = mutableSetOf()
        private val stack: Stack<String> = Stack()
        private var xpathState = XPathState.IGNORE
        private var countingArguments = false

        var context = false
        val varlist: MutableSet<String> = mutableSetOf()
        val funclist: MutableSet<Pair<String,Int>> = mutableSetOf()

        fun initialize() {
            input = ""
            context = false
            varlist.clear()
            funclist.clear()
            stack.clear()
            sawDollar = false
            functionCall = false
            functionName = false
        }

        override fun reset(string: CharSequence?) {
            if (trace) {
                println("Parser reset: ${string}")
            }
            input = string?.toString() ?: ""
        }

        override fun startNonterminal(name: String?, begin: Int) {
            when (name) {
                "PathExpr" -> xpathState = XPathState.PATHEXPR
                "RelativePathExpr" -> {
                    if (xpathState == XPathState.PATHEXPR) {
                        xpathState = XPathState.RELPATHEXPR
                    } else {
                        xpathState = XPathState.IGNORE
                    }
                }
                "StepExpr" -> {
                    if (xpathState == XPathState.RELPATHEXPR) {
                        xpathState = XPathState.STEPEXPR
                    } else {
                        xpathState = XPathState.IGNORE
                    }
                }
                "AxisStep" -> {
                    if (xpathState == XPathState.STEPEXPR) {
                        context = true
                    }
                    xpathState = XPathState.IGNORE
                }
                "PostfixExpr" -> {
                    if (xpathState == XPathState.STEPEXPR) {
                        xpathState = XPathState.POSTFIXEXPR
                    } else {
                        xpathState = XPathState.IGNORE
                    }
                }
                "PrimaryExpr" -> {
                    if (xpathState == XPathState.POSTFIXEXPR) {
                        xpathState = XPathState.PRIMARYEXPR
                    } else {
                        xpathState = XPathState.IGNORE
                    }
                }
                "ContextItemExpr" -> {
                    if (xpathState == XPathState.PRIMARYEXPR) {
                        context = true
                        xpathState = XPathState.IGNORE
                    }
                }
                "ArgumentList" -> {
                    countingArguments = true
                }
            }

            if (trace) {
                println("+NT: ${name} (${xpathState})")
            }
            stack.push(name)

            when (name) {
                "FunctionCall" -> functionCall = true
                "FunctionName" -> functionName = true
                "FunctionEQName" -> functionName = true
                "QuantifiedExpr" -> quantified = true
                else -> Unit
            }
        }

        override fun endNonterminal(name: String?, end: Int) {
            if (trace) {
                println("-NT: ${name}")
                if (name == "XPath") {
                    println("--- CONTEXT=${context}")
                }
            }
            stack.pop()

            when (name) {
                "FunctionCall" -> functionCall = false
                "FunctionName" -> functionName = false
                "FunctionEQName" -> functionName = false
                "Argument" -> {
                    if (countingArguments) {
                        val fcall = functionStack.pop()
                        functionStack.push(Pair(fcall.first, fcall.second+1))
                    }
                }
                "ArgumentList" -> {
                    val fcall = functionStack.pop()
                    if (trace) {
                        println("Function call: ${fcall.first}#${fcall.second}")
                    }
                    funclist.add(fcall)
                    countingArguments = false
                }
                else -> Unit
            }
        }

        override fun terminal(name: String?, begin: Int, end: Int) {
            if (trace) {
                println("  T: ${name}")
            }

            if (xpathState == XPathState.PATHEXPR && (name == "'/'" || name == "'//'")) {
                context = true
            }

            if (sawDollar) {
                val varname = characters(begin, end)
                if (quantified) {
                    quantvar += varname
                } else {
                    if (!quantvar.contains(varname)) {
                        varlist += varname
                    }
                }
            } else {
                if (functionCall && functionName) {
                    functionStack.push(Pair(characters(begin, end), 0))
                }
            }
            sawDollar = name == "'$'"
            if (quantified && name == "in") {
                quantified = false
            }
        }

        override fun whitespace(begin: Int, end: Int) {
            // nop
        }

        private fun characters(begin: Int, end: Int): String =
            if (begin < end) {
                input.substring(begin, end)
            } else {
                ""
            }
    }
}