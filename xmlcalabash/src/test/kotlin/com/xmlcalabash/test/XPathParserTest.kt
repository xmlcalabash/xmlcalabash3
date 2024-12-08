package com.xmlcalabash.test

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.InstructionConfiguration
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.parsers.XPathExpressionDetails
import com.xmlcalabash.parsers.xpath31.XPathExpressionParser
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class XPathParserTest {
    companion object {
        val a = QName("a")
        val b = QName("b")
        val f_1 = Pair(QName(NsFn.namespace, "f"), 1)
        val f_2 = Pair(QName(NsFn.namespace, "f"), 2)
        val ex_f_1 = Pair(QName(NamespaceUri.of("http://example.com/"), "f"), 1)
    }

    lateinit var stepConfig: InstructionConfiguration

    @BeforeAll
    fun init() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        stepConfig = builder.stepConfig.with("ex", NamespaceUri.of("http://example.com/"))
    }

    fun assertVariables(varlist: List<QName>, details: XPathExpressionDetails): Unit {
        Assertions.assertEquals(varlist.size, details.variableRefs.size)
        for (variable in varlist) {
            Assertions.assertTrue(details.variableRefs.contains(variable))
        }
    }

    fun assertFunctions(funclist: List<Pair<QName,Int>>, details: XPathExpressionDetails): Unit {
        Assertions.assertEquals(funclist.size, details.functionRefs.size)
        for (func in funclist) {
            var found = false
            for (dfunc in details.functionRefs) {
                found = func.first == dfunc.first && func.second == dfunc.second
                if (found) {
                    break
                }
            }
            Assertions.assertTrue(found)
        }
    }

    @Test
    fun smokeTest() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("1 + 1")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun simplePathExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("a")
        Assertions.assertTrue(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun simplePathExprEQName() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("Q{}a")
        Assertions.assertTrue(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun simplePathAdditionExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("a + 3")
        Assertions.assertTrue(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun contextItem() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse(". + 3")
        Assertions.assertTrue(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }


    @Test
    fun contextItemInPredicate() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("\$a[. = '3']")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        assertVariables(listOf(a), details)
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun contextItemTopLevel() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse(".?headers?content-type")
        Assertions.assertTrue(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }


    @Test
    fun pathExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("a/b")
        Assertions.assertTrue(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun rootPathExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("/a/b")
        Assertions.assertTrue(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun simpleVariableReference() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("\$a")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        assertVariables(listOf(a), details)
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun variablePathExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("\$a/b")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        assertVariables(listOf(a), details)
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun variableRef() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("\$a + 1")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertEquals(1, details.variableRefs.size)
        Assertions.assertTrue(details.variableRefs.contains(a))
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun quantifiedExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("some \$a in 1 satisfies \$a = \$b")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        assertVariables(listOf(b), details)
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun doublyQuantifiedExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("some \$a in (1, 2, 3), \$b in ('a','b','c') satisfies \$a = \$b")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun letExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("let \$a := 3 return \$a")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun letabExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("let \$a := 3, \$b := 3 return \$a + \$b")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun letabForwardRefExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("let \$a := \$b, \$b := 3 return \$a + \$b")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        assertVariables(listOf(b), details)
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun nestedLetExpr() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("let \$a := 3 return let \$b := 3 return \$a + \$b")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun functionRef() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("f(1)")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        Assertions.assertTrue(details.variableRefs.isEmpty())
        assertFunctions(listOf(f_1), details)
        Assertions.assertNull(details.error)
    }

    @Test
    fun inlineFunctionRef() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("let \$f := function(\$a, \$b) { \$a + \$b } return f(\$a,\$b)")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        assertVariables(listOf(a, b), details)
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

    @Test
    fun inlineFunctionSequenceRef() {
        val parser = XPathExpressionParser(stepConfig)
        val details = parser.parse("let \$f := (1, function(\$a, \$b) { \$a + \$b }) return \$f[2](\$a,\$b)")
        Assertions.assertFalse(details.contextRef)
        Assertions.assertFalse(details.alwaysDynamic)
        assertVariables(listOf(a, b), details)
        Assertions.assertTrue(details.functionRefs.isEmpty())
        Assertions.assertNull(details.error)
    }

}