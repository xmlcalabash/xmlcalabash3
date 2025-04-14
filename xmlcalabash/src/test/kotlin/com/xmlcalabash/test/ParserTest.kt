package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.parsers.xpl.XplParser
import org.junit.jupiter.api.Test

class ParserTest {
    @Test
    fun parseLibrary1() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val parser = XplParser(builder)
        val pipeline = parser.parse(UriUtils.cwdAsUri().resolve("src/test/resources/parser/library1.xpl"))
        println(pipeline)
    }

    @Test
    fun importFunctions1() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val parser = XplParser(builder)
        val decl = parser.parse(UriUtils.cwdAsUri().resolve("src/test/resources/parser/importfunctions.xpl"))
        val pipeline = decl.getExecutable()
        val receiver = BufferingReceiver()
        pipeline.receiver = receiver
        pipeline.run()
        if (receiver.outputs["result"] != null) {
            for (doc in receiver.outputs["result"]!!) {
                println(doc.value)
            }
        }
    }
}