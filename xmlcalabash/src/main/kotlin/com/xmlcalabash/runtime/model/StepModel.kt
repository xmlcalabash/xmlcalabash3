package com.xmlcalabash.runtime.model

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.datamodel.StaticOptionDetails
import com.xmlcalabash.graph.Model
import com.xmlcalabash.graph.ModelInputPort
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import com.xmlcalabash.runtime.steps.AbstractStep
import net.sf.saxon.s9api.QName

abstract class StepModel(val runtime: XProcRuntime, model: Model) {
    val stepConfig = model.step.stepConfig.copy()
    val id: String = model.id
    val location: Location = model.step.location
    val name: String = model.step.name
    val type: QName = model.step.instructionType
    internal val _inputs: MutableMap<String, RuntimePort>
    val inputs: Map<String, RuntimePort>
        get() = _inputs
    internal val _outputs: MutableMap<String, RuntimePort>
    val outputs: Map<String, RuntimePort>
        get() = _outputs
    val options: Map<QName, RuntimeOption>

    internal val extensionAttributes = mutableMapOf<QName, String>()
    internal val staticOptions = mutableMapOf<QName, StaticOptionDetails>()

    init {
        val ports = mutableMapOf<String, RuntimePort>()
        for ((name, port) in model.inputs) {
            val rtport = RuntimePort(name, port.unbound, port.sequence, port.contentTypes)
            rtport.weldedShut = port.weldedShut
            if (port is ModelInputPort) {
                rtport.defaultBindings.addAll(port.defaultBindings)
            }
            ports[name] = rtport
        }
        _inputs = ports.toMutableMap()

        ports.clear()
        for ((name, port) in model.outputs) {
            val rtport = RuntimePort(name, port.unbound, port.sequence, port.contentTypes)
            rtport.weldedShut = port.weldedShut
            ports[name] = rtport
        }
        _outputs = ports.toMutableMap()

        val roptions = mutableMapOf<QName, RuntimeOption>()
        for ((name, option) in model.options) {
            roptions[name] = RuntimeOption(name, option.asType, option.values, option.staticValue)
        }
        options = roptions.toMap()
    }

    internal abstract fun initialize(model: Model)
    internal abstract fun runnable(yconfig: RuntimeStepConfiguration): ()  -> AbstractStep

    override fun toString(): String {
        return "${type}"
    }
}