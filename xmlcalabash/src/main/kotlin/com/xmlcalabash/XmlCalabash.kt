package com.xmlcalabash

import com.xmlcalabash.datamodel.CompileEnvironment
import com.xmlcalabash.datamodel.PipelineBuilder
import com.xmlcalabash.parsers.xpl.XplParser

class XmlCalabash internal constructor(val config: XmlCalabashConfiguration): XmlCalabashConfiguration by config {
    companion object {
        fun newInstance(): XmlCalabash {
            return XmlCalabashBuilder().build()
        }
    }

    init {
        val environment = CompileEnvironment("", this)
        config.saxonConfiguration.environment = environment
    }

    fun newPipelineBuilder(): PipelineBuilder {
        val builder = PipelineBuilder.newInstance(this)
        return builder
    }

    fun newPipelineBuilder(version: Double): PipelineBuilder {
        val builder = PipelineBuilder.newInstance(this, version)
        return builder
    }

    fun newXProcParser(): XplParser {
        return XplParser(newPipelineBuilder())
    }

    fun newXProcParser(builder: PipelineBuilder): XplParser {
        return XplParser(builder)
    }
}