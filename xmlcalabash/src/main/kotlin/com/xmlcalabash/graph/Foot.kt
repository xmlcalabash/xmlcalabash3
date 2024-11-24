package com.xmlcalabash.graph

class Foot(graph: Graph, parent: CompoundModel): Model(graph, parent, parent.step) {
    override fun init() {
        // nop
    }

    override fun toString(): String {
        return "Foot(${parent})"
    }
}