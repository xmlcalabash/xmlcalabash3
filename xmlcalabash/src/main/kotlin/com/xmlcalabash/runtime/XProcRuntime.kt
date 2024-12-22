package com.xmlcalabash.runtime

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.graph.*
import com.xmlcalabash.namespace.NsDescription
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode

class XProcRuntime private constructor(internal val start: DeclareStepInstruction, internal val config: XProcStepConfiguration) {
    companion object {
        internal fun newInstance(start: DeclareStepInstruction): XProcRuntime {
            val environment = PipelineContext(start.stepConfig.environment as PipelineCompilerContext)
            val config = XProcStepConfigurationImpl(environment, start.stepConfig.saxonConfig, start.location)
            val runtime = XProcRuntime(start, config)
            val usedSteps = runtime.findUsedSteps(start)
            val pipelines = mutableMapOf<DeclareStepInstruction, SubpipelineModel>()
            for (decl in usedSteps) {
                val model = SubpipelineModel(Graph.build(decl, config.environment))
                model.init()
                pipelines[decl] = model
            }
            runtime.initialize(start, pipelines)
            return runtime
        }
    }

    internal lateinit var pipelines: Map<DeclareStepInstruction, SubpipelineModel>
    internal lateinit var pipelineStep: CompoundStepModel
    internal val runnables = mutableMapOf<String, CompoundStepModel>()
    val environment = config.environment

    fun stepConfiguration(instructionConfig: InstructionConfiguration): XProcStepConfiguration {
        val impl = XProcStepConfigurationImpl(config.environment, instructionConfig.saxonConfig.newConfiguration(), instructionConfig.location)
        impl.putAllNamespaces(instructionConfig.inscopeNamespaces)
        impl.putAllStepTypes(instructionConfig.inscopeStepTypes)
        return impl
    }

    fun executable(): XProcPipeline {
        val config = config.copy()
        return XProcPipeline(this, pipelineStep, config)
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

        val pipelineModels = mutableMapOf<CompoundModel, CompoundStepModel>()

        for ((decl, model) in pipelines) {
            val graph = model.graph
            val smodel = graph.models.filterIsInstance<SubpipelineModel>().first { it.model is PipelineModel }

            val pipelineUserStep = CompoundStepModel(this, smodel.model)  // YAtomicCompoundStep(this, smodel)
            runnables[smodel.step.id] = pipelineUserStep
            if (decl.type == start.type) {
                pipelineStep = pipelineUserStep
            }

            pipelineModels[smodel.model] = pipelineUserStep
        }

        for ((model, step) in pipelineModels) {
            step.initialize(model)
        }
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