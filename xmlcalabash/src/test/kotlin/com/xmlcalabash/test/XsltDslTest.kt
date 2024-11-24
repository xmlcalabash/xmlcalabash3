package com.xmlcalabash.test

import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.Processor
import org.junit.jupiter.api.Test

class XsltDslTest {
    @Test
    fun smokeTest() {
        val builder = SaxonTreeBuilder(Processor(false))
        val x = builder.xslt(ns = mapOf("xs" to "http://www.w3.org/2001/XMLSchema")) {
            mode(onNoMatch = "shallow-copy") {}
            template(match="/") {
                applyTemplates() {}
                text() { +"Something here" }
            }
        }
        println(x)
    }
}