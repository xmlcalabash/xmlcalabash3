package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.exceptions.XProcError

open class AtomicModel(graph: Graph, parent: Model, step: AtomicStepInstruction, id: String): Model(graph, parent, step, id) {
    override fun init() {
        graph.models.add(this)
        graph.instructionMap[step] = this

        for (child in step.children) {
            when (child) {
                is WithInputInstruction -> {
                    inputs[child.port] = if (!child.weldedShut && (child.children.isEmpty() && child.defaultBindings.isNotEmpty())) {
                        ModelInputPort(this, child)
                    } else {
                        ModelPort(this, child)
                    }
                    for (conn in child.children) {
                        when (conn) {
                            is PipeInstruction -> {
                                val from = graph.instructionMap[conn.readablePort!!.parent]!!
                                val to = graph.instructionMap[step]!!
                                graph.addEdge(from, conn.port!!, to, child.port)
                            }
                            else -> {
                                throw step.stepConfig.exception(XProcError.xiImpossible("Connection is not a pipe: ${conn}"))
                            }
                        }
                    }
                }
                is WithOutputInstruction -> {
                    outputs[child.port] = ModelPort(this, child)
                }
                is WithOptionInstruction -> {
                    options[child.name] = ModelOption(this, child)
                }
                else -> Unit
            }
        }

        for ((name, value) in step.staticOptions) {
            options[name] = ModelOption(this, value)
        }
    }

    override fun toString(): String {
        return step.toString()
    }
}