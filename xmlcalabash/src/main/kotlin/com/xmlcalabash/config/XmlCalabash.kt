package com.xmlcalabash.config

import com.xmlcalabash.datamodel.PipelineBuilder
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.util.DefaultXmlCalabashConfiguration
import com.xmlcalabash.parsers.xpl.XplParser
import com.xmlcalabash.runtime.RuntimeExecutionContext
import com.xmlcalabash.runtime.XProcExecutionContext
import java.util.*

// XmlCalabash and SaxonConfiguration are intertwingled in an unappealing way
class XmlCalabash private constructor(val xmlCalabashConfig: XmlCalabashConfiguration) {
    private lateinit var _saxonConfig: SaxonConfiguration

    val saxonConfig: SaxonConfiguration
        get() = _saxonConfig

    companion object {
        fun newInstance(): XmlCalabash {
            return newInstance(DefaultXmlCalabashConfiguration())
        }

        fun newInstance(config: XmlCalabashConfiguration): XmlCalabash {
            val xmlCalabash = XmlCalabash(config)
            val rteContext = RuntimeExecutionContext(xmlCalabash)
            val saxonConfig = SaxonConfiguration.newInstance(xmlCalabash, rteContext)
            xmlCalabash._saxonConfig = saxonConfig
            config.xmlCalabashConfigurer(xmlCalabash)
            return xmlCalabash
        }
    }

    fun newPipelineBuilder(): PipelineBuilder {
        return PipelineBuilder.newInstance(saxonConfig.newConfiguration())
    }

    fun newPipelineBuilder(version: Double): PipelineBuilder {
        return PipelineBuilder.newInstance(saxonConfig.newConfiguration(), version)
    }

    fun newXProcParser(): XplParser {
        return newXProcParser(newPipelineBuilder())
    }

    fun newXProcParser(builder: PipelineBuilder): XplParser {
        return XplParser(builder)
    }

    private val executables = mutableMapOf<Long, Stack<XProcExecutionContext>>()
    private val episodes = mutableMapOf<Long,String>()

    internal fun getEpisode(): String {
        return episodes[Thread.currentThread().id] ?: ""
    }

    internal fun setEpisode(episode: String) {
        episodes[Thread.currentThread().id] = episode
    }

    internal fun newExecutionContext(stepConfig: XProcStepConfiguration): XProcExecutionContext {
        synchronized(executables) {
            var stack = executables[Thread.currentThread().id]
            val context = if (stack == null) {
                stack = Stack()
                executables[Thread.currentThread().id] = stack
                XProcExecutionContext(stepConfig)
            } else {
                if (stack.isEmpty()) {
                    XProcExecutionContext(stepConfig)
                } else {
                    XProcExecutionContext(stack.peek()!!)
                }
            }
            stack.push(context)
            //println("New ${this}: ${context} for ${Thread.currentThread().id} (${stack.size})")
            return context
        }
    }

    internal fun getExecutionContext(): XProcExecutionContext {
        synchronized(executables) {
            val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
            //println("Get ${this}: ${stack.peek()} for ${Thread.currentThread().id} (${stack.size})")
            return stack.peek()
        }
    }

    internal fun setExecutionContext(dynamicContext: XProcExecutionContext) {
        synchronized(executables) {
            var stack = executables[Thread.currentThread().id]
            if (stack == null) {
                stack = Stack()
                executables[Thread.currentThread().id] = stack
            }
            stack.push(dynamicContext)
            //println("Set ${this}: ${dynamicContext} for ${Thread.currentThread().id} (${stack.size})")
        }
    }

    internal fun releaseExecutionContext() {
        synchronized(executables) {
            val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
            val context = stack.pop()
            //println("Rel ${this}: ${context} for ${Thread.currentThread().id} (${stack.size})")
        }
    }

    internal fun discardExecutionContext() {
        synchronized(executables) {
            val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
            stack.clear()
            //println("Discard ${this}: ${context} for ${Thread.currentThread().id}")
        }
    }

    internal fun addProperties(doc: XProcDocument?) {
        if (doc == null) {
            return
        }
        val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
        stack.peek()!!.addProperties(doc)
    }

    internal fun removeProperties(doc: XProcDocument?) {
        if (doc == null) {
            return
        }
        val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
        stack.peek()!!.removeProperties(doc)
    }
}