package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.PipelineBuilder
import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XPathSelector
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.trans.XPathException
import net.sf.saxon.value.BooleanValue

class UseWhenContext internal constructor(val builder: PipelineBuilder) {
    companion object {
        private var id = 0
    }
    var unknownStepTypes = false
    val staticOptions = mutableMapOf<OptionNode, XdmValue?>()
    val stepTypes = mutableMapOf<QName, StepImplementation>()
    val useWhen = mutableSetOf<ElementNode>()
    var resolvedCount = 0
    val contextId = ++id

    init {
        for (child in builder.standardLibrary.children.filterIsInstance<DeclareStepInstruction>()) {
            if (child.type != null) {
                stepTypes[child.type!!] = StepImplementation(true, child.stepConfig.rteContext.atomicStepAvailable(child.type!!))
            }
        }
    }

    fun copy(): UseWhenContext {
        val context = UseWhenContext(builder)
        context.unknownStepTypes = unknownStepTypes
        context.staticOptions.putAll(staticOptions)
        context.stepTypes.putAll(stepTypes)
        context.useWhen.clear()
        context.resolvedCount = 0
        return context
    }

    fun resolveExpression(stepConfig: StepConfiguration, expr: String): XdmValue? {
        return resolveExpressionInternal(stepConfig, expr, false)
    }

    fun resolveUseWhen(stepConfig: StepConfiguration, expr: String): Boolean? {
        val result = resolveExpressionInternal(stepConfig, expr, true)
        if (result != null) {
            return (result.underlyingValue as BooleanValue).booleanValue
        }
        return null
    }

    private fun resolveExpressionInternal(stepConfig: StepConfiguration, expr: String, ebv: Boolean): XdmValue? {
        val context = ConditionalExecutionContext(stepConfig, this)
        stepConfig.setExecutionContext(context)
        try {
            val selector = makeSelector(stepConfig, expr)
            if (ebv) {
                val result = selector.effectiveBooleanValue()
                return XdmAtomicValue(result)
            } else {
                val result = selector.evaluate()
                return result
            }
        } catch (ex: Exception) {
            when (ex) {
                is ConditionalStepException -> Unit
                is SaxonApiException -> {
                    if (!conditionalOption(stepConfig, ex)) {
                        throw XProcError.xsXPathStaticError(ex.message ?: "").exception(ex)
                    }
                }
                else -> Unit // Just assume this will work better next time...
            }
        } finally {
            stepConfig.releaseExecutionContext()
        }
        return null
    }

    private fun makeSelector(stepConfig: StepConfiguration, expr: String): XPathSelector {
        val processor = stepConfig.saxonConfig.processor

        val compiler = processor.newXPathCompiler()
        for ((prefix, uri) in stepConfig.inscopeNamespaces) {
            compiler.declareNamespace(prefix, uri.toString())
        }
        for ((name, value) in builder.staticOptionsManager.useWhenOptions) {
            compiler.declareVariable(name)
        }

        val selector = compiler.compile(expr).load()
        for ((name, value) in builder.staticOptionsManager.useWhenOptions) {
            selector.setVariable(name, value)
        }
        return selector
    }

    private fun conditionalOption(stepConfig: StepConfiguration, ex: SaxonApiException): Boolean {
        val cause = ex.cause
        val message = ex.message
        if (cause is XPathException && message != null) {
            val code = cause.showErrorCode()
            if (code == "XPST0008") {
                val pos = message.indexOf("\$")
                if (pos > 0) {
                    val name = message.substring(pos+1)
                    val qname = if (name.startsWith("{")) {
                        // What fresh hell is this? The message contains "${uri}local"!?
                        val cpos = name.indexOf("}")
                        QName("", name.substring(1, cpos), name.substring(cpos+1))
                    } else {
                        stepConfig.parseQName(name)
                    }
                    for ((option, _) in staticOptions) {
                        if (option.name == qname) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    override fun toString(): String {
        return "UseWhenContext: ${contextId}"
    }
}