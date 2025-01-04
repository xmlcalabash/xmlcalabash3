package com.xmlcalabash.io

import com.xmlcalabash.documents.DocumentContext
import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

abstract class Marshaller(val docContext: DocumentContext) {
    companion object {
        private val _a = QName("a")
        private val _b = QName("b")
    }

    protected fun runFunction(function: String, values: List<XdmValue>): XdmValue {
        var compiler = docContext.processor.newXPathCompiler()

        when (values.size) {
            0 -> {
                val selector = compiler.compile("${function}()").load()
                return selector.evaluate()
            }
            1 -> {
                compiler.declareVariable(_a)
                val selector = compiler.compile("${function}(\$a)").load()
                selector.setVariable(QName("a"), values[0])
                return selector.evaluate()
            }
            2 -> {
                compiler.declareVariable(_a)
                compiler.declareVariable(_b)
                val selector = compiler.compile("${function}(\$a, \$b)").load()
                selector.setVariable(QName("a"), values[0])
                selector.setVariable(QName("b"), values[1])
                return selector.evaluate()
            }
            else -> {
                throw XProcError.xiImpossible("Called runFunction with more than two arguments").exception()
            }
        }
    }
}