package com.xmlcalabash.test

import com.xmlcalabash.model.util.XProcConstants.ValueTemplate
import com.xmlcalabash.util.ValueTemplateParser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{a, be}

class ValueTemplateParserSpec extends AnyFlatSpec /* with Matchers */ {
  "Parsing |test|" should "succeed" in {
    val parser = new ValueTemplateParser("test")
    val template = parser.template()
    assertCorrect(template, List("test"))
  }

  "Parsing |test {expr}|" should "succeed" in {
    val parser = new ValueTemplateParser("test {expr}")
    val template = parser.template()
    assertCorrect(template, List("test ", "expr"))
  }

  "Parsing |{concat('{', $foo, '}')}|" should "succeed" in {
    val parser = new ValueTemplateParser("{concat('{', $foo, '}')}")
    val template = parser.template()
    assertCorrect(template, List("", "concat('{', $foo, '}')"))
  }

  "Parsing |{concat( (: I can write anything here, even }, '}', and \"}\" :) $foo, '}')}|" should "succeed" in {
    val parser = new ValueTemplateParser("{concat( (: I can write anything here, even }, '}', and \"}\" :) $foo, '}')}")
    val template = parser.template()
    assertCorrect(template, List("", "concat( (: I can write anything here, even }, '}', and \"}\" :) $foo, '}')"))
  }

  "Parsing |{concat($foo, '}')}|" should "succeed" in {
    val parser = new ValueTemplateParser("{concat($foo, '}')}")
    val template = parser.template()
    assertCorrect(template, List("", "concat($foo, '}')"))
  }

  "Parsing |{{{concat('{', $foo, '}')}}}|" should "succeed" in {
    val parser = new ValueTemplateParser("{{{concat('{', $foo, '}')}}}")
    val template = parser.template()
    assertCorrect(template, List("{", "concat('{', $foo, '}')", "}"))
  }

  "Parsing |{concat( { not really valid XPath } )}|" should "succeed" in {
    val parser = new ValueTemplateParser("{concat( { not really valid XPath } )}")
    val template = parser.template()
    assertCorrect(template, List("", "concat( { not really valid XPath } )"))
  }

  "Parsing |{p:system-property('Q{someURI}localname')}|" should "succeed" in {
    val parser = new ValueTemplateParser("{p:system-property('Q{someURI}localname')}")
    val template = parser.template()
    assertCorrect(template, List("", "p:system-property('Q{someURI}localname')"))
  }

  "Parsing |Q{}error|" should "succeed" in {
    val parser = new ValueTemplateParser("{Q{}error}")
    val template = parser.template()
    assertCorrect(template, List("", "Q{}error"))
  }

  it should "throw RuntimeException parsing |{test|" in {
    val parser = new ValueTemplateParser("{test")
    a [RuntimeException] should be thrownBy {
      parser.template()
    }
  }

  it should "throw RuntimeException parsing |{|" in {
    val parser = new ValueTemplateParser("{")
    a [RuntimeException] should be thrownBy {
      parser.template()
    }
  }

  it should "throw RuntimeException parsing |test }|" in {
    val parser = new ValueTemplateParser("test }")
    a [RuntimeException] should be thrownBy {
      parser.template()
    }
  }

  private def assertCorrect(template: ValueTemplate, expected: List[String]): Unit = {
    assert(template.size == expected.size)
    for (pos <- template.indices) {
      assert(template(pos) == expected(pos))
    }
  }
}
