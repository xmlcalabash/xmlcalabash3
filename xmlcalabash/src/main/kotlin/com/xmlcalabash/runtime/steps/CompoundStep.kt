package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.RuntimeEnvironment
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.AtomicBuiltinOptionModel
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.runtime.model.StepModel
import com.xmlcalabash.runtime.parameters.OptionStepParameters
import com.xmlcalabash.steps.internal.ExpressionStep
import kotlinx.coroutines.*
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger
import kotlin.collections.mutableMapOf

abstract class CompoundStep(config: XProcStepConfiguration, compound: CompoundStepModel): AbstractStep(config, compound) {
    companion object {
        fun newInstance(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep {
            val newConfig = compound.stepConfig.copy()
            when (compound.type) {
                NsP.declareStep -> return PipelineStep(newConfig, compound)
                NsP.group -> return GroupStep(newConfig, compound)
                NsP.forEach -> return ForEachStep(newConfig, compound)
                NsP.viewport -> return ViewportStep(newConfig, compound)
                NsP.choose -> return ChooseStep(newConfig, compound)
                NsP.`when` -> return ChooseWhenStep(newConfig, compound)
                NsP.otherwise -> return ChooseOtherwiseStep(newConfig, compound)
                NsP.`if` -> return ChooseStep(newConfig, compound)
                NsP.`try` -> return TryStep(newConfig, compound)
                NsP.catch -> return TryCatchStep(newConfig, compound)
                NsP.finally -> return TryFinallyStep(newConfig, compound)
                NsP.run -> return RunStep(newConfig, compound)
                NsCx.`while` -> return WhileStep(newConfig, compound)
                NsCx.until -> return UntilStep(newConfig, compound)
                else -> throw config.exception(XProcError.xiImpossible("Unsupported compound step type: ${compound.type}"))
            }
        }
    }

    final override val params = compound.params
    val inputPorts = mutableSetOf<String>()

    private val extensionAttributes = compound.extensionAttributes.toList()
    internal val runnableProviders = mutableMapOf<StepModel, () -> AbstractStep>()
    internal val edges = compound.edges
    internal val runnables = mutableListOf<AbstractStep>()
    internal val head = CompoundStepHead(config, this, compound.head)
    internal val foot = CompoundStepFoot(config, this, compound.foot)
    internal var stepName: String? = null
    internal var stepType: QName? = null
    override val stepTimeout = compound.timeout
    internal val childThreadGroups = compound.childThreadGroups
    internal var iterationPosition = 0L
    internal var iterationSize = 0L
    protected val stepsToRun = mutableListOf<AbstractStep>()
    private var exception: Throwable? = null

    private var showedReady = true
    override val readyToRun: Boolean
        get() {
            val ready = head.readyToRun
            if (ready) {
                logger.debug { "READY ${this}" }
                showedReady = true
            } else {
                if (showedReady) {
                    logger.debug { "NORDY ${this}" }
                    showedReady = false
                }
            }
            return ready
        }

    init {
        inputPorts.addAll(compound.inputs.keys)
        head.staticOptions.putAll(staticOptions)

        runnableProviders[compound.head] = { head }
        runnableProviders[compound.foot] = { foot }
        for (step in compound.steps) {
            val stepRunnable = step.runnable(config)
            runnableProviders[step] = stepRunnable
        }
    }

    override fun instantiate() {
        val runnableMap = mutableMapOf<StepModel, AbstractStep>()
        for ((model, provider) in runnableProviders) {
            val stepRunnable = provider()
            runnableMap[model] = stepRunnable
            if (stepRunnable !is CompoundStepHead && stepRunnable !is CompoundStepFoot) {
                runnables.add(stepRunnable)
            }
        }

        for (edge in edges) {
            val from = runnableMap[edge.fromStep]!!
            val to = runnableMap[edge.toStep]!!

            val rfrom = if (from is CompoundStep) {
                from.foot
            } else {
                from
            }

            val rto = if (to is CompoundStep) {
                to.head
            } else {
                to
            }

            rfrom.receiver[edge.fromPort] = Pair(rto, edge.toPort)
            if (from is ViewportStep || from is TryStep || from is ChooseWhenStep) {
                from.foot.holdPorts.add(edge.fromPort)
            }
        }
    }

    override fun prepare() {
        if (runnables.isEmpty()) {
            instantiate()
        }
    }

    override fun run() {
    }

    protected fun runSubpipeline() {
        if (stepsToRun.isEmpty()) {
            return
        }

        var maxThreads = Int.MAX_VALUE
        for (pair in extensionAttributes) {
            if (pair.first == NsCx.maxThreadCount) {
                maxThreads = pair.second.toInt()
            }
        }

        var threadsToUse = 1
        var threadsConsumed = 0
        var threadsAvailable = 0
        synchronized(stepConfig.environment) {
            if (stepConfig.environment is RuntimeEnvironment) {
                maxThreads = Math.min(maxThreads, stepConfig.environment.threadsAvailable)
                threadsAvailable = maxThreads
                threadsToUse = Math.max(1, Math.min(threadsAvailable, childThreadGroups))
                if (threadsToUse > 1 && stepConfig.environment.threadsAvailable > 1) {
                    threadsConsumed = threadsToUse
                    stepConfig.environment.threadsAvailable -= threadsConsumed
                }
            }
        }

        try {
            if (threadsToUse == 1) {
                val atomicOptionValues = mutableMapOf<QName, LazyValue>()
                if (threadsAvailable > 0) {
                    logger.debug {
                        var sep = ": "
                        val sb = StringBuilder()
                        sb.append("Single threading")
                        for (step in stepsToRun) {
                            sb.append(sep).append(step.name)
                            sep = " → "
                        }
                        sb.toString()
                    }
                }
                runSequentialStepsExhaustively(atomicOptionValues, stepsToRun)
            } else {
                logger.debug { "Assigning ${threadsToUse}/${threadsAvailable} threads to ${childThreadGroups} thread groups" }
                val groups = assignToThreads(threadsToUse)
                for ((index, group) in groups.withIndex()) {
                    logger.debug {
                        var sep = ": "
                        val sb = StringBuilder()
                        sb.append("Thread ${index+1}")
                        for (step in group) {
                            sb.append(sep).append(step.name)
                            sep = " → "
                        }
                        sb.toString()
                    }
                }
                runConcurrentStepsExhaustively(groups)
            }
        } catch (ex: XProcException) {
            if (ex.error.code == NsErr.threadInterrupted) {
                for (step in stepsToRun) {
                    step.abort()
                }
            }
            throw ex
        } finally {
            synchronized(stepConfig.environment) {
                (stepConfig.environment as RuntimeEnvironment).threadsAvailable += threadsConsumed
            }
        }
    }

    private fun assignToThreads(threadsAvailable: Int): List<List<AbstractStep>> {
        var groups = mutableListOf<MutableList<Int>>()
        for (num in 0 ..< childThreadGroups) {
            groups.add(mutableListOf())
            for ((index, step) in stepsToRun.withIndex()) {
                if (step.threadGroup == 0) {
                    throw IllegalStateException("Step not assigned a thread group: ${step}")
                }
                if (step.threadGroup == num+1) {
                    groups[num].add(index)
                }
            }
        }

        // If we're running some subset of steps that doesn't include every group,
        // discard the empty groups
        val collapsedGroups = mutableListOf<MutableList<Int>>()
        for (group in groups) {
            if (group.isNotEmpty()) {
                collapsedGroups.add(group)
            }
        }

        groups = collapsedGroups
        while (groups.size > threadsAvailable) {
            groups = combineTwoGroups(groups)
        }

        val threadedSteps = mutableListOf<MutableList<AbstractStep>>()
        for (group in groups) {
            val list = mutableListOf<AbstractStep>()
            for (index in group) {
                list.add(stepsToRun[index])
            }
            threadedSteps.add(list)
        }

        return threadedSteps
    }

    private fun combineTwoGroups(groups: MutableList<MutableList<Int>>): MutableList<MutableList<Int>> {
        // Find the two shortest groups...
        var firstIndex = -1
        var firstSize = Int.MAX_VALUE
        var secondIndex = -1
        var secondSize = Int.MAX_VALUE

        for ((index, group) in groups.withIndex()) {
            if (group.size <= secondSize) {
                secondSize = group.size
                secondIndex = index
            }
        }

        for ((index, group) in groups.withIndex()) {
            if (index != secondIndex && group.size <= firstSize) {
                firstSize = group.size
                firstIndex = index
            }
        }

        val newGroups = mutableListOf<MutableList<Int>>()
        for ((index, group) in groups.withIndex()) {
            when (index) {
                firstIndex -> {
                    val list = mutableListOf<Int>()
                    list.addAll(groups[index])
                    list.addAll(groups[secondIndex])
                    list.sortWith(Comparator { a, b -> a.compareTo(b) })
                    newGroups.add(list)
                }
                secondIndex -> Unit
                else -> {
                    val list = mutableListOf<Int>()
                    list.addAll(groups[index])
                    newGroups.add(list)
                }
            }
        }

        return newGroups
    }

    override fun input(port: String, doc: XProcDocument) {
        throw UnsupportedOperationException("Never send an input to a compound step")
    }

    override fun output(port: String, document: XProcDocument) {
        throw UnsupportedOperationException("Never send an output to a compound step")
    }

    override fun close(port: String) {
        throw UnsupportedOperationException("Never close an input on a compound step")
    }

    override fun reset() {
        super.reset()
        head.reset()
        foot.reset()
        for (step in runnables) {
            step.reset()
        }
    }

    private fun runConcurrentStepsExhaustively(groups: List<List<AbstractStep>>) {
        // When we're running a step defined by a pipeline, the options passed in or computed
        // must be available for computing subsequent options.
        val atomicOptionValues = mutableMapOf<QName, LazyValue>()

        runBlocking {
            val handler = CoroutineExceptionHandler { context, ex ->
                exception = ex
            }

            val jobs: List<Job> = groups.map { group ->
                launch (Dispatchers.Default + handler + SupervisorJob()) {
                    runSequentialStepsExhaustively(atomicOptionValues, group)
                }
            }
            jobs.forEach { it.join() }
        }

        if (exception != null) {
            throw exception!!
        }
    }

    private fun runSequentialStepsExhaustively(atomicOptionValues: MutableMap<QName, LazyValue>, steps: List<AbstractStep>) {
        val stepsToRun = mutableListOf<AbstractStep>()
        stepsToRun.addAll(steps)

        // When we're running a step defined by a pipeline, the options passed in or computed
        // must be available for computing subsequent options.

        for (runMe in steps) {
            while (!runMe.readyToRun) {
                if (exception != null) {
                    // This must be in the threaded case and something's gone wrong on another thread
                    return
                }
                Thread.sleep(20)
            }

            if (runMe is AtomicOptionStep) {
                synchronized(atomicOptionValues) {
                    runMe.atomicOptionValues.putAll(atomicOptionValues)
                }
                runMe.runStep(this)
                synchronized(atomicOptionValues) {
                    if (runMe.externalValue == null) {
                        atomicOptionValues[runMe.externalName] = LazyValue(runMe.stepConfig, (runMe.implementation as ExpressionStep).expression, stepConfig)
                    } else {
                        atomicOptionValues[runMe.externalName] = LazyValue(XProcDocument.ofValue(runMe.externalValue!!.value, runMe.stepConfig), stepConfig)
                    }
                }
            } else {
                runMe.runStep(this)
            }

            if (exception != null) {
                // This must be in the threaded case and something's gone wrong on another thread
                return
            }
        }
    }

    override fun toString(): String {
        val dispType = stepType ?: type
        val dispName = stepName ?: name
        return "${dispType}/${dispName} (${id})"
    }
}