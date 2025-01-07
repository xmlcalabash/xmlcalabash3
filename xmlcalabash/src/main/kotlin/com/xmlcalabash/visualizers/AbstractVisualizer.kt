package com.xmlcalabash.visualizers

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.api.Monitor
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.Consumer
import org.apache.logging.log4j.kotlin.logger
import java.util.Stack

abstract class AbstractVisualizer(val options: Map<String,String>): Monitor {
    val stacks = mutableMapOf<Long, Stack<AbstractStep>>()

    abstract fun showStart(step: AbstractStep, depth: Int)
    abstract fun showEnd(step: AbstractStep, depth: Int)
    abstract fun showDocument(step: AbstractStep, port: String, depth: Int, document: XProcDocument)

    override fun startStep(step: AbstractStep) {
        var depth = 0
        synchronized(stacks) {
            val stack = stacks[Thread.currentThread().id] ?: Stack()
            stacks[Thread.currentThread().id] = stack
            stack.push(step)
            depth = stack.size
        }
        showStart(step, depth)
    }

    override fun endStep(step: AbstractStep) {
        var depth = 0
        synchronized(stacks) {
            val stack = stacks[Thread.currentThread().id]!!
            depth = stack.size
            stack.pop()
        }
        showEnd(step, depth)
    }

    override fun abortStep(step: AbstractStep, ex: Exception) {
        // ignore
    }

    override fun sendDocument(from: Pair<AbstractStep, String>, to: Pair<Consumer, String>, document: XProcDocument): XProcDocument {
        var depth = 0
        synchronized(stacks) {
            val stack = stacks[Thread.currentThread().id] ?: Stack()
            stacks[Thread.currentThread().id] = stack
            for (step in stack) {
                depth++
                if (step === from.first) {
                    break
                }
            }
        }
        showDocument(from.first, from.second, depth, document)
        return document
    }

    protected fun boolean(key: String, value: String): Boolean {
        if (value == "true" || value == "false") {
            return value == "true"
        }
        logger.warn("Option value for ${key} must be true or false, not \"${value}\"")
        return false
    }
}
