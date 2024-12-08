package com.xmlcalabash.test

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.namespace.NsXs
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExpressionSelectTest: Expressions() {
    @Test
    public fun simpleLiterals() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        val stepConfig = builder.stepConfig.copy()
        val asType = stepConfig.parseSequenceType(type("integer"))
        val expr = XProcExpression.select(stepConfig, "1+2", asType, false, emptyList())
        val result = expr.evaluate(stepConfig)
        println(result)
    }

    @Test
    public fun simpleStaticVariables() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        val stepConfig = builder.stepConfig.copy()
        val asType = stepConfig.parseSequenceType(type("integer"))
        val expr = XProcExpression.select(stepConfig, "\$a+\$b", asType, false, emptyList())
        expr.setStaticBinding(QName("a"), XProcExpression.constant(stepConfig, XdmAtomicValue(17)))
        expr.setStaticBinding(QName("b"), XProcExpression.constant(stepConfig, XdmAtomicValue(3)))
        val resultFunction = expr.xevaluate(stepConfig)

        Assertions.assertTrue(expr.canBeResolvedStatically())
        val result = resultFunction()
        Assertions.assertEquals(result, XdmAtomicValue(20))
    }

    @Test
    public fun simpleVariables() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        val stepConfig = builder.stepConfig.copy()
        val asType = stepConfig.parseSequenceType(type("integer"))
        val expr = XProcExpression.select(stepConfig, "\$a+\$b", asType, false, emptyList())
        expr.setStaticBinding(QName("a"), XProcExpression.constant(stepConfig, XdmAtomicValue(17)))
        val resultFunction = expr.xevaluate(stepConfig)

        Assertions.assertFalse(expr.canBeResolvedStatically())
        val result = if (expr.canBeResolvedStatically()) {
            resultFunction()
        } else {
            expr.setBinding(QName("b"), XdmAtomicValue(3))
            resultFunction()
        }

        println(result)
    }

    @Test
    public fun unboundVariable() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        val stepConfig = builder.stepConfig.copy()
        val asType = stepConfig.parseSequenceType(type("integer"))
        val expr = XProcExpression.select(stepConfig, "\$a+\$b", asType, false, emptyList())
        expr.setStaticBinding(QName("a"), XProcExpression.constant(stepConfig, XdmAtomicValue(17)))
        Assertions.assertFalse(expr.canBeResolvedStatically())
    }
}