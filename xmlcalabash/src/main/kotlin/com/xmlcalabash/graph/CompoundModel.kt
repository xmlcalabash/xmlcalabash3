package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.XdmMap

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
                                graph.addEdge(from, conn.port!!, to, child.port, conn.implicit)
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
                                footPort.assertions.addAll(child.assertions)
                                footPort.serialization = child.serialization.evaluate(child.stepConfig) as XdmMap
                                foot.inputs[child.port] = footPort

                                graph.addEdge(from, conn.port!!, foot, child.port, conn.implicit)
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
            port.assertions.addAll(input.assertions)

            val toEdges = graph.edges.filter { it.to == child }
            for (edge in toEdges) {
                val iport = edge.inputPort
                graph.addEdge(edge.from, edge.outputPort, submodel, iport, edge.implicit)
                graph.edges.remove(edge)
                val mport = ModelPort(child.inputs[iport]!!)
                submodel.inputs[iport] = ModelPort(submodel, iport, false, mport.primary, mport.sequence, mport.contentTypes)
            }
        }

        for (output in child.outputs.values.toList()) {
            val fromEdges = graph.edges.filter { it.from == child }
            for (edge in fromEdges) {
                val oport = edge.outputPort
                graph.addEdge(submodel, oport, edge.to, edge.inputPort, edge.implicit)
                graph.edges.remove(edge)
                val mport = ModelPort(child.outputs[oport]!!)
                val outport = ModelPort(submodel, oport, false, mport.primary, mport.sequence, mport.contentTypes)
                outport.assertions.addAll(output.assertions)
                submodel.outputs[oport] = outport
            }
        }

        return submodel
    }

    internal fun computeOrder() {
        when (step.instructionType) {
            NsP.choose -> computeChooseOrder()
            NsP.`when` -> computeWhenOrder()
            NsP.`try` -> computeTryOrder()
            else -> {
                val orderedChildren = computePipelineOrder(children)
                _children.clear()
                addOrderedChildren(orderedChildren)
            }
        }
    }

    private fun computeChooseOrder() {
        // The when/otherwise steps come in order at the end, everything else is ordered
        val other = mutableListOf<Model>()
        val wotherwise = mutableListOf<Model>()
        for (child in children) {
            if (child.step.instructionType in listOf(NsP.`when`, NsP.otherwise)) {
                wotherwise.add(child)
            } else {
                other.add(child)
            }
        }

        _children.clear()
        addOrderedChildren(computePipelineOrder(other))
        addOrderedChildren(wotherwise)
    }

    private fun computeWhenOrder() {
        val save = mutableListOf<Model>()
        save.addAll(children)

        // Annoyingly, sometimes spitters and joiners are necessary for
        // the guard expression or something that leads up to it. They
        // don't necessarily come before the cx:guard, so we have to do
        // this the hard way...
        val guard = children.first { it.step.instructionType == NsCx.guard }
        _children.remove(guard)

        val before = mutableListOf<Model>()

        var done = false
        while (!done) {
            done = true

            var move: Model? = null
            for (child in children) {
                for (edge in graph.edges.filter { it.from == child }) {
                    if (edge.to == guard || edge.to in before) {
                        move = child
                        break
                    }
                }
            }

            if (move != null) {
                done = false
                before.add(move)
                _children.remove(move)
            }
        }

        val after = mutableListOf<Model>()
        after.addAll(children)

        _children.clear()
        addOrderedChildren(computePipelineOrder(before))
        _children.add(guard)
        addOrderedChildren(computePipelineOrder(after, before))
    }

    private fun computeTryOrder() {
        // The catch/finally steps come in order at the end, everything else is ordered
        val other = mutableListOf<Model>()
        val cfinally = mutableListOf<Model>()
        for (child in children) {
            if (child.step.instructionType in listOf(NsP.catch, NsP.finally)) {
                cfinally.add(child)
            } else {
                other.add(child)
            }
        }

        _children.clear()
        addOrderedChildren(computePipelineOrder(other))
        addOrderedChildren(cfinally)
    }

    private fun computePipelineOrder(subpipeline: List<Model>, preceding: List<Model> = emptyList()): List<Model> {
        val orderedChildren = mutableListOf<Model>()
        val precedingChildren = mutableListOf<Model>()
        val allChildren = mutableListOf<Model>()
        allChildren.addAll(subpipeline)

        precedingChildren.add(head)
        precedingChildren.addAll(preceding)

        while (allChildren.isNotEmpty()) {
            val selectedChildren = mutableListOf<Model>()
            for (child in allChildren) {
                var wait = false
                for (edge in graph.edges) {
                    if (edge.to == child) {
                        if (edge.from !in precedingChildren) {
                            wait = true
                            break
                        }
                    }
                }
                if (!wait) {
                    selectedChildren.add(child)
                    orderedChildren.add(child)
                }
            }
            if (selectedChildren.isEmpty()) {
                throw step.stepConfig.exception(XProcError.xiImpossible("Failed to find a partial order for all steps"))
            }
            precedingChildren.addAll(selectedChildren)
            for (child in selectedChildren) {
                allChildren.remove(child)
            }
            selectedChildren.clear()
        }

        return orderedChildren
    }

    private fun addOrderedChildren(orderedChildren: List<Model>) {
        for (child in orderedChildren) {
            if (child is SubpipelineModel) {
                child.model.computeOrder()
            }
            _children.add(child)
        }
    }

    override fun toString(): String {
        return step.toString()
    }
}