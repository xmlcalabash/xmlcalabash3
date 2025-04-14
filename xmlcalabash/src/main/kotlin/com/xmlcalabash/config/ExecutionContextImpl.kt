package com.xmlcalabash.config

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcExecutionContext
import net.sf.saxon.s9api.XdmValue
import java.util.Stack

class ExecutionContextImpl: ExecutionContextManager {
    private val executables = mutableMapOf<Long, Stack<XProcExecutionContext>>()

    override fun newExecutionContext(stepConfig: StepConfiguration): XProcExecutionContext {
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

    override fun getExecutionContext(): XProcExecutionContext {
        synchronized(executables) {
            val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
            //println("Get ${this}: ${stack.peek()} for ${Thread.currentThread().id} (${stack.size})")
            return stack.peek()
        }
    }

    override fun setExecutionContext(dynamicContext: XProcExecutionContext) {
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

    override fun releaseExecutionContext() {
        synchronized(executables) {
            val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
            val context = stack.pop()
            //println("Rel ${this}: ${context} for ${Thread.currentThread().id} (${stack.size})")
        }
    }

    override fun preserveExecutionContext(): Stack<XProcExecutionContext> {
        synchronized(executables) {
            val stack = executables[Thread.currentThread().id] ?: Stack()
            val saveStack = Stack<XProcExecutionContext>()
            while (stack.isNotEmpty()) {
                saveStack.push(stack.pop())
            }
            return saveStack
        }
    }

    override fun restoreExecutionContext(contextStack: Stack<XProcExecutionContext>) {
        synchronized(executables) {
            var stack: Stack<XProcExecutionContext>? = executables[Thread.currentThread().id]
            if (stack == null) {
                stack = Stack()
                executables[Thread.currentThread().id] = stack
            }
            stack.clear()
            while (contextStack.isNotEmpty()) {
                stack.push(contextStack.pop())
            }
        }
    }

    override fun addProperties(doc: XProcDocument?) {
        if (doc == null) {
            return
        }
        val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
        stack.peek()!!.addProperties(doc)
    }

    override fun removeProperties(doc: XProcDocument?) {
        if (doc == null) {
            return
        }
        val stack: Stack<XProcExecutionContext> = executables[Thread.currentThread().id]!!
        stack.peek()!!.removeProperties(doc)
    }
}