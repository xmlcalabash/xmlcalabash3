package com.xmlcalabash.test

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.DefaultErrorExplanation
import org.scalatest.flatspec.AnyFlatSpec

class ErrorExplainIL10nSpec extends AnyFlatSpec {
  "Parsing " should " use the default locale" in {
    val explain = new DefaultErrorExplanation()
    val text = explain.message(XProcException.err_xd0059, 1, List("irrelevant"))
    assert(text.contains("Invalid option:"))
  }

  "Parsing with testlang " should " use the testlang locale" in {
    System.setProperty("user.language", "testlang")
    val explain = new DefaultErrorExplanation()
    val text = explain.message(XProcException.err_xd0059, 1, List("irrelevant"))
    assert(text.contains("Invalid option testlang:"))
  }

  "Parsing with testlang_testcountry " should " use the testlang_testcountry locale" in {
    System.setProperty("user.language", "testlang")
    System.setProperty("user.country", "testcountry")
    val explain = new DefaultErrorExplanation()
    val text = explain.message(XProcException.err_xd0059, 1, List("irrelevant"))
    assert(text.contains("Invalid option testlang_testcountry:"))
  }
}
