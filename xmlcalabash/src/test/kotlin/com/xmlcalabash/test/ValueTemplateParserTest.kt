package com.xmlcalabash.test

import com.xmlcalabash.util.ValueTemplate
import com.xmlcalabash.util.ValueTemplateParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.lang.RuntimeException

internal class ValueTemplateParserTest {
    @Test
    fun parseTest() {
        val template = ValueTemplateParser.parse("test")
        assertEquals(ValueTemplate(listOf("test")), template)
    }

    @Test
    fun parseTestExpr() {
        val template = ValueTemplateParser.parse("test {expr}")
        assertEquals(ValueTemplate(listOf("test ", "expr")), template)
    }

    @Test
    fun parseDollarFoo() {
        val template = ValueTemplateParser.parse("{concat('{', \$foo, '}')}")
        assertEquals(ValueTemplate(listOf("", "concat('{', \$foo, '}')")), template)

    }

    @Test
    fun parseTest4() {
        val template = ValueTemplateParser.parse("{concat( (: I can write anything here, even }, '}', and \"}\" :) \$foo, '}')}")
        assertEquals(ValueTemplate(listOf("", "concat( (: I can write anything here, even }, '}', and \"}\" :) \$foo, '}')")), template)
    }

    @Test
    fun parseTest5() {
        val template = ValueTemplateParser.parse("{concat(\$foo, '}')}")
        assertEquals(ValueTemplate(listOf("", "concat(\$foo, '}')")), template)
    }

    @Test
    fun parseTest6() {
        val template = ValueTemplateParser.parse("{{{concat('{', \$foo, '}')}}}")
        assertEquals(ValueTemplate(listOf("{", "concat('{', \$foo, '}')", "}")), template)
    }

    @Test
    fun parseTest7() {
        val template = ValueTemplateParser.parse("{concat( { not really valid XPath } )}")
        assertEquals(ValueTemplate(listOf("", "concat( { not really valid XPath } )")), template)
    }

    @Test
    fun parseTest8() {
        val template = ValueTemplateParser.parse("{p:system-property('Q{someURI}localname')}")
        assertEquals(ValueTemplate(listOf("", "p:system-property('Q{someURI}localname')")), template)
    }

    @Test
    fun parseTest9() {
        val template = ValueTemplateParser.parse("{Q{}error}")
        assertEquals(ValueTemplate(listOf("", "Q{}error")), template)
    }

    @Test
    fun parseTest10() {
        try {
            ValueTemplateParser.parse("{test")
            fail()
        } catch (ex: RuntimeException) {
            // pass
        }
    }

    @Test
    fun parseTest11() {
        try {
            ValueTemplateParser.parse("{")
            fail()
        } catch (ex: RuntimeException) {
            // pass
        }
    }

    @Test
    fun parseTest12() {
        try {
            ValueTemplateParser.parse("test }")
            fail()
        } catch (ex: RuntimeException) {
            // pass
        }
    }

    @Test
    fun parseTest13() {
        val template = ValueTemplateParser.parse("{ (:(: :) } :) 3 (: { :) }")
        assertEquals(ValueTemplate(listOf("", " (:(: :) } :) 3 (: { :) ")), template)
    }

}