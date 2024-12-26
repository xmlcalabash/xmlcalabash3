package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP

open class CompoundModel internal constructor(graph: Graph, parent: Model?, step: CompoundStepDeclaration, id: String): Model(graph, parent, step, id) {
    val timeout = step.timeout

    private val _head = Head(graph, this, "${id}_head")
    val head: Head
        get() = _head

    private val _foot = Foot(graph, this, "${id}_foot")
    val foot: Foot
        get() = _foot

    internal val _children = mutableListOf<Model>()
    val children: List<Model>
        get() = _children

    override fun init() {
        graph.models.add(this)
        graph.instructionMap[step] = this

        head.init()
        for (child in children) {
            child.init()
        }
        foot.init()

        for (option in (step as CompoundStepDeclaration).options) {
            options[option.name] = ModelOption(this, option)
        }

        for (child in step.children) {
            when (child) {
                is InputInstruction -> { // current on viewport and for-each
                    inputs[child.port] = ModelInputPort(this, child)
                    head.outputs[child.port] = ModelInputPort(head, child)
                }
                is WithInputInstruction -> {
                    inputs[child.port] = ModelPort(this, child)
                    when (step) {
                        is CompoundLoopDeclaration, is ChooseInstruction -> {
                            head.inputs[child.port] = ModelPort(this, child)
                        }
                        else -> {
                            head.outputs[child.port] = ModelPort(this, child)
                        }
                    }
                }
                is OutputInstruction -> {
                    outputs[child.port] = ModelPort(foot, child)
                    //head.inputs[child.port] = ModelPort(head, child)
                    foot.inputs[child.port] = ModelPort(foot, child)
                }
                else -> Unit
            }
        }

        addEdges()
    }

    private fun addEdges() {
        for (child in step.children) {
            when (child) {
                is StepDeclaration -> Unit // Unit, we'll call this method for all nodes
                is InputInstruction -> Unit // current on for-each and viewport
                is WithInputInstruction -> {
                    for (conn in child.children) {
                        when (conn) {
                            is PipeInstruction -> {
                                val from = graph.instructionMap[conn.readablePort!!.parent]!!
                                val to = graph.instructionMap[step]!!
                                graph.addEdge(from, conn.port!!, to, child.port)
                            }
                            else -> {
                                throw XProcError.xiImpossible("Connection is not a pipe: ${conn}").exception()
                            }
                        }
                    }
                }
                is OutputInstruction -> {
                    for (conn in child.children) {
                        when (conn) {
                            is PipeInstruction -> {
                                val from = graph.instructionMap[conn.readablePort!!.parent]!!
                                val footPort = ModelPort(foot, child.port, false, child.primary == true, child.sequence == true, child.contentTypes)
                                footPort.schematron.addAll(child.schematron)
                                foot.inputs[child.port] = footPort

                                graph.addEdge(from, conn.port!!, foot, child.port)
                            }
                            else -> {
                                throw XProcError.xiImpossible("Connection is not a pipe: ${conn}").exception()
                            }
                        }
                    }
                }
                is WithOutputInstruction -> {
                    outputs[child.port] = ModelPort(this, child)
                }
                is RunOptionInstruction -> Unit // only on p:run which is only a quasi-compound step
                is WithOptionInstruction -> {
                    options[child.name] = ModelOption(this, child)
                }
                else -> TODO("Unexpected child: ${child}")
            }
        }
    }

    internal open fun decompose() {
        for (index in children.indices) {
            val child = children[index]
            if (child is CompoundModel) {
                val submodel = decomposeCompoundModel(child)
                _children[index] = submodel
                child.decompose()
            }
        }
    }

    protected fun decomposeCompoundModel(child: CompoundModel): SubpipelineModel {
        val submodel = SubpipelineModel(child, "${child.id}_pipeline")
        submodel.init()

        for (input in child.inputs.values.toList()) {
            val port = if (input.name.startsWith("!cache")) {
                ModelPort(submodel, input.name, false, false, true, listOf())
            } else {
                ModelPort(submodel, child.step.namedInput(input.name)!!)
            }
            port.schematron.addAll(input.schematron)

            val toEdges = graph.edges.filter { it.to == child }
            for (edge in toEdges) {
                val iport = edge.inputPort
                graph.addEdge(edge.from, edge.outputPort, submodel, iport)
                graph.edges.remove(edge)
                val mport = ModelPort(child.inputs[iport]!!)
                submodel.inputs[iport] = ModelPort(submodel, iport, false, mport.primary, mport.sequence, mport.contentTypes)
            }
        }

        for (output in child.outputs.values.toList()) {
            val fromEdges = graph.edges.filter { it.from == child }
            for (edge in fromEdges) {
                val oport = edge.outputPort
                graph.addEdge(submodel, oport, edge.to, edge.inputPort)
                graph.edges.remove(edge)
                val mport = ModelPort(child.outputs[oport]!!)
                val outport = ModelPort(submodel, oport, false, mport.primary, mport.sequence, mport.contentTypes)
                outport.schematron.addAll(output.schematron)
                submodel.outputs[oport] = outport
            }
        }

        return submodel
    }

    override fun toString(): String {
        return step.toString()
    }
}