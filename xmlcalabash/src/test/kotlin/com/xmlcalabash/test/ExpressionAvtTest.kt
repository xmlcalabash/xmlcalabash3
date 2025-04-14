package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.datamodel.XProcExpression
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExpressionAvtTest: XmlCalabashTestClass() {
    @Test
    public fun simpleLiterals() {
        val xmlCalabash = XmlCalabashBuilder().build()
        val builder = xmlCalabash.newPipelineBuilder()
        val stepConfig = builder.stepConfig.copy()
        val asType = SequenceType.ANY
        val expr = XProcExpression.avt(stepConfig, "Three: {1+2}", asType, emptyList())
        val resultFunction = expr.xevaluate(stepConfig)
        val result = resultFunction()
        Assertions.assertEquals("Three: 3", result.underlyingValue.stringValue)
    }

    @Test
    public fun typedLiteral() {
        val xmlCalabash = XmlCalabashBuilder().build()
        val builder = xmlCalabash.newPipelineBuilder()
        val stepConfig = builder.stepConfig.copy()
        val asType = stepConfig.typeUtils.parseSequenceType(type("integer"))
        val expr = XProcExpression.avt(stepConfig, "{1+2}", asType, emptyList())
        val resultFunction = expr.xevaluate(stepConfig)
        val result = resultFunction()
        Assertions.assertEquals(XdmAtomicValue(3), result)
    }
}