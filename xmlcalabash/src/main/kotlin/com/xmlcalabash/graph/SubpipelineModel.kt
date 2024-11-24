package com.xmlcalabash.graph

class SubpipelineModel(val model: CompoundModel): Model(model.graph, model, model.step) {
    override fun init() {
        graph.models.add(this)
        graph.instructionMap[step] = this
    }

    override fun toString(): String {
        return "S(${model})"
    }
}