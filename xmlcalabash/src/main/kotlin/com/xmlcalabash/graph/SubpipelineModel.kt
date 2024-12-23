package com.xmlcalabash.graph

class SubpipelineModel(val model: CompoundModel, id: String): Model(model.graph, model, model.step, id) {
    override fun init() {
        graph.models.add(this)
        graph.instructionMap[step] = this
    }

    override fun toString(): String {
        return "S(${model})"
    }
}