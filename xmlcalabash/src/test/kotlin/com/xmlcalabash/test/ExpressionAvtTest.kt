package com.xmlcalabash.test

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.namespace.NsXs
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExpressionAvtTest: Expressions() {
    @Test
    public fun simpleLiterals() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        val stepConfig = builder.stepConfig.copy()
        val asType = SequenceType.ANY
        val expr = XProcExpression.avt(stepConfig, "Three: {1+2}", asType, emptyList())
        val resultFunction = expr.xevaluate()
        val result = resultFunction()
        Assertions.assertEquals("Three: 3", result.underlyingValue.stringValue)
    }

    @Test
    public fun typedLiteral() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        val stepConfig = builder.stepConfig.copy()
        val asType = stepConfig.parseSequenceType(type("integer"))
        val expr = XProcExpression.avt(stepConfig, "{1+2}", asType, emptyList())
        val resultFunction = expr.xevaluate()
        val result = resultFunction()
        Assertions.assertEquals(XdmAtomicValue(3), result)
    }

}