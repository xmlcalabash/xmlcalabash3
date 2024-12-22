package com.xmlcalabash.config

import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.PipelineBuilder
import com.xmlcalabash.datamodel.PipelineCompilerContext
import com.xmlcalabash.datamodel.StandardLibrary
import com.xmlcalabash.datamodel.Visibility
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.util.DefaultXmlCalabashConfiguration
import com.xmlcalabash.parsers.xpl.XplParser
import com.xmlcalabash.runtime.XProcExecutionContext
import com.xmlcalabash.runtime.XProcStepConfiguration
import java.util.*
import kotlin.collections.set

// XmlCalabash and SaxonConfiguration are intertwingled in an unappealing way
class XmlCalabash private constructor(val xmlCalabashConfig: XmlCalabashConfiguration) {
    private lateinit var _saxonConfig: SaxonConfiguration
    private lateinit var _commonEnvironment: CommonEnvironment

    val saxonConfig: SaxonConfiguration
        get() = _saxonConfig

    val commonEnvironment: CommonEnvironment
        get() = _commonEnvironment

    companion object {
        fun newInstance(): XmlCalabash {
            return newInstance(DefaultXmlCalabashConfiguration())
        }

        fun newInstance(config: XmlCalabashConfiguration): XmlCalabash {
            val xmlCalabash = XmlCalabash(config)
            xmlCalabash._commonEnvironment = CommonEnvironment(xmlCalabash)
            val environment = PipelineCompilerContext(xmlCalabash)
            val saxonConfig = SaxonConfiguration.newInstance(environment)
            xmlCalabash._saxonConfig = saxonConfig
            config.xmlCalabashConfigurer(xmlCalabash)

            val library = StandardLibrary.getInstance(environment, saxonConfig)
            for (decl in library.children.filterIsInstance<DeclareStepInstruction>()) {
                if (decl.type != null && decl.visibility != Visibility.PRIVATE) {
                    environment.commonEnvironment._standardSteps[decl.type!!] = decl
                }
            }

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
            val stack: Stack<XProcExecutionContext>? = executables[Thread.currentThread().id]
            if (stack != null) {
                stack.clear()
            }
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