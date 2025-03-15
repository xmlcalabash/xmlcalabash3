package com.xmlcalabash.graph

class Edge(val number: Long, val from: Model, val outputPort: String, val to: Model, val inputPort: String, val implicit: Boolean) {
    override fun toString(): String {
        return "${from}.${outputPort} -> ${to}.${inputPort}"
    }
}