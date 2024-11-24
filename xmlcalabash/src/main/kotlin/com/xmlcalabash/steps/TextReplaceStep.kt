package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.parameters.StepParameters
import net.sf.saxon.s9api.XdmAtomicValue

open class TextReplaceStep(): AbstractTextStep() {
    override fun run() {
        super.run()

        val pattern = stringBinding(Ns.pattern)
        val replacement = stringBinding(Ns.replacement)
        val flags = stringBinding(Ns.flags) ?: ""
        val text = text()

        val compiler = stepConfig.processor.newXPathCompiler()
        compiler.declareVariable(Ns.pattern)
        compiler.declareVariable(Ns.replacement)
        compiler.declareVariable(Ns.text)
        compiler.declareVariable(Ns.flags)

        try {
            val selector = compiler.compile("replace(\$text, \$pattern, \$replacement, \$flags)").load()
            selector.resourceResolver = stepConfig.documentManager
            selector.setVariable(Ns.pattern, XdmAtomicValue(pattern))
            selector.setVariable(Ns.replacement, XdmAtomicValue(replacement))
            selector.setVariable(Ns.text, XdmAtomicValue(text))
            selector.setVariable(Ns.flags, XdmAtomicValue(flags))
            val result = selector.evaluate()

            receiver.output("result", source.with(result))
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdStepFailed("Invalid regex: ${pattern}"))
        }
    }

    override fun toString(): String = "p:text-count"
}