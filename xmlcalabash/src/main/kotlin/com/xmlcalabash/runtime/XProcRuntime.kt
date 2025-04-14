package com.xmlcalabash.runtime

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.graph.*
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.util.S9Api
import net.sf.saxon.Configuration
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.XdmNodeKind
import org.xml.sax.InputSource
import javax.xml.transform.sax.SAXSource

class XProcRuntime private constructor(internal val start: DeclareStepInstruction, internal val config: XProcStepConfiguration) {
    companion object {
        val uniqueNames = mutableListOf<String>()

        internal fun newInstance(start: DeclareStepInstruction): XProcRuntime {
            val runtimeEnvironment = RuntimeEnvironment.newInstance(start.stepConfig.environment as CompileEnvironment)
            val saxonConfig = start.stepConfig.saxonConfig.newConfiguration()
            val config = XProcStepConfiguration(saxonConfig, start.stepConfig.context.copy(), runtimeEnvironment)

            if (start.psviRequired == true && !saxonConfig.configuration.isLicensedFeature(Configuration.LicenseFeature.SCHEMA_VALIDATION)) {
                throw XProcError.xdPsviUnsupported().exception()
            }

            val runtime = XProcRuntime(start, config)
            val usedSteps = runtime.findUsedSteps(start)

            for (step in usedSteps) {
                for (info in step.pipeinfo) {
                    for (element in S9Api.documentElement(info).axisIterator(Axis.CHILD)) {
                        if (element.nodeKind == XdmNodeKind.ELEMENT) {
                            when (element.nodeName) {
                                NsCx.useCatalog -> {
                                    val href = element.getAttributeValue(Ns.href)
                                    if (href == null) {
                                        config.warn { "Ignoring ${element.nodeName}: missing href" }
                                    } else {
                                        val uri = element.baseURI.resolve(href).toString()
                                        config.debug { "Adding catalog: ${uri}" }
                                        config.environment.documentManager.resolverConfiguration.addCatalog(uri)
                                    }
                                }
                                NsCx.importSchema -> {
                                    val href = element.getAttributeValue(Ns.href)
                                    if (href == null) {
                                        config.warn { "Ignoring ${element.nodeName}: missing href" }
                                    } else {
                                        config.saxonConfig.addSchemaDocument(element.baseURI.resolve(href))
                                    }
                                }
                                else -> {
                                    if (element.nodeName.namespaceUri == NsCx.namespace) {
                                        config.warn { "Unexpected element in p:pipeinfo: ${element.nodeName}" }
                                    }
                                }
                            }
                        }
                    }
                }
            }


            val pipelines = mutableMapOf<DeclareStepInstruction, SubpipelineModel>()
            for (decl in usedSteps) {
                val model = Graph.build(decl)
                model.init()
                pipelines[decl] = model
            }
            runtime.initialize(start, pipelines)
            return runtime
        }
    }

    internal val graphList = mutableListOf<Graph>()
    internal lateinit var pipelines: Map<DeclareStepInstruction, SubpipelineModel>
    internal lateinit var pipelineStep: CompoundStepModel
    internal val runnables = mutableMapOf<String, CompoundStepModel>()
    val environment = config.environment

    fun stepConfiguration(instructionConfig: InstructionConfiguration): XProcStepConfiguration {
        // Issue #160, don't create a new Saxon configuration here
        val impl = config.from(instructionConfig, false)
        impl.validationMode = instructionConfig.validationMode
        return impl
    }

    fun executable(): XProcPipeline {
        val config = config.copy()
        return XProcPipeline(this, pipelineStep, config)
    }

    private fun pipelineFilename(pipeline: DeclareStepInstruction): String {
        val name = if (pipeline.name.startsWith("!")) {
            (pipeline.type ?: pipeline.instructionType).toString().replace(":", "_")
        } else {
            pipeline.name
        }

        var unique = name
        var count = 1
        while (uniqueNames.contains(unique)) {
            count++
            unique = "${name}_${count}"
        }
        uniqueNames.add(unique)
        return unique
    }

    fun description(): XProcDescription {
        val description = XProcDescription(pipelineStep.stepConfig)

        // Work out what all the filenames will be so that the linking is obvious
        val filenameMap = mutableMapOf<String, String>()
        for ((decl, model) in pipelines) {
            filenameMap[decl.id] = pipelineFilename(decl)
        }

        // Make sure the starting point is the first pipeline/graph
        description.addPipeline(PipelineVisualization.build(start, filenameMap))

        val startmodel = pipelines[start]!!
        description.addGraph(GraphVisualization.build(startmodel.graph, startmodel.model as PipelineModel, filenameMap))

        for ((decl, model) in pipelines) {
            if (decl !== start) {
                description.addPipeline(PipelineVisualization.build(decl, filenameMap))
                description.addGraph(GraphVisualization.build(model.graph, model.model as PipelineModel, filenameMap))
            }
        }

        return description
    }

    private fun initialize(start: DeclareStepInstruction, pipelines: Map<DeclareStepInstruction, SubpipelineModel>) {
        this.pipelines = pipelines

        val pipelineModels = mutableMapOf<CompoundModel, CompoundStepModel>()

        for ((decl, model) in pipelines) {
            val graph = model.graph
            val smodel = graph.models.filterIsInstance<SubpipelineModel>().first { it.model is PipelineModel }
            graphList.add(graph)

            val pipelineUserStep = CompoundStepModel(this, smodel.model)
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