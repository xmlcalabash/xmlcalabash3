package com.xmlcalabash.graph

class Foot(graph: Graph, parent: CompoundModel, id: String): Model(graph, parent, parent.step, id) {
    override fun init() {
        // nop
    }

    override fun toString(): String {
        return "Foot(${parent})"
    }
}