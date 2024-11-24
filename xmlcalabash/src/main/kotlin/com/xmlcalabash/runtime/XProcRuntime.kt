package com.xmlcalabash.runtime

import com.xmlcalabash.datamodel.AtomicStepInstruction
import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.PipelineVisualization
import com.xmlcalabash.datamodel.StepDeclaration
import com.xmlcalabash.graph.*
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsDescription
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XProcRuntime private constructor(private val start: DeclareStepInstruction) {
    companion object {
        private val anonymous = QName(NsCx.namespace, "cx:anonymous")

        fun newInstance(start: DeclareStepInstruction): XProcRuntime {
            val runtime = XProcRuntime(start)
            val usedSteps = runtime.findUsedSteps(start)
            val pipelines = mutableMapOf<DeclareStepInstruction, SubpipelineModel>()
            for (decl in usedSteps) {
                val model = SubpipelineModel(Graph.build(decl))
                model.init()
                pipelines[decl] = model
            }
            runtime.initialize(start, pipelines)
            return runtime
        }
    }

    private lateinit var pipelines: Map<DeclareStepInstruction, SubpipelineModel>
    private lateinit var pipelineStep: CompoundStepModel
    internal val yrunnables = mutableMapOf<QName, CompoundStepModel>()

    fun executable(): XProcPipeline {
        return XProcPipeline(pipelineStep)
    }

    fun description(): XdmNode {
        val builder = SaxonTreeBuilder(pipelineStep.stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsDescription.description)

        builder.addSubtree(PipelineVisualization.build(start))

        val startmodel = pipelines[start]!!
        val startxml = GraphVisualization.build(startmodel.graph, startmodel.model as PipelineModel)
        builder.addSubtree(startxml)

        for ((decl, model) in pipelines) {
            if (decl !== start) {
                builder.addSubtree(PipelineVisualization.build(decl))
                builder.addSubtree(GraphVisualization.build(model.graph, model.model as PipelineModel))
            }
        }

        builder.addEndElement()
        builder.endDocument()
        return builder.result
    }

    private fun initialize(start: DeclareStepInstruction, pipelines: Map<DeclareStepInstruction, SubpipelineModel>) {
        this.pipelines = pipelines

        val ypipelineModels = mutableMapOf<CompoundModel, CompoundStepModel>()

        for ((decl, model) in pipelines) {
            val graph = model.graph
            val smodel = graph.models.filterIsInstance<SubpipelineModel>().filter { it.model is PipelineModel }.first()

            val ypipelineUserStep = CompoundStepModel(this, smodel.model)  // YAtomicCompoundStep(this, smodel)
            // There can be at most one anonymous pipeline (the start one)
            yrunnables[decl.type ?: anonymous] = ypipelineUserStep

            ypipelineModels[smodel.model] = ypipelineUserStep
        }

        for ((model, step) in ypipelineModels) {
            step.initialize(model)
        }

        pipelineStep = yrunnables[start.type ?: anonymous]!!
    }

    private fun findUsedSteps(start: DeclareStepInstruction, seen: MutableSet<DeclareStepInstruction> = mutableSetOf()): Set<DeclareStepInstruction> {
        if (seen.contains(start)) {
            return seen
        }
        seen.add(start)
        for (child in start.children.filterIsInstance<StepDeclaration>()) {
            findUsedSteps(seen, child)
        }
        return seen
    }

    private fun findUsedSteps(seen: MutableSet<DeclareStepInstruction>, step: StepDeclaration) {
        if (step is AtomicStepInstruction) {
            val decl = step.declaration()!!
            if (!decl.isAtomic) {
                findUsedSteps(decl, seen)
            }
            return
        }

        for (child in step.children.filterIsInstance<StepDeclaration>()) {
            findUsedSteps(seen, child)
        }
    }
}