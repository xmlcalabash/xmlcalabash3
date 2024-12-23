package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.InputInstruction

class Head(graph: Graph, parent: CompoundModel, id: String): Model(graph, parent, parent.step, id) {
    val defaultInputs = mutableMapOf<String,DefaultInput>()

    override fun init() {
        for (input in step.inputs().filterIsInstance<InputInstruction>()) {
            if (input.defaultBindings.isNotEmpty()) {
                val default = DefaultInput(input.defaultBindings, input.select)
                defaultInputs[input.port] = default
            }
        }
    }

    override fun toString(): String {
        return "Head(${parent})"
    }
}