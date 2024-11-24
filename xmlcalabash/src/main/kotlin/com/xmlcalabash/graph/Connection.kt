package com.xmlcalabash.graph

class Connection(var from: ModelPort, var to: ModelPort) {
    override fun toString(): String {
        return "${from} -> ${to}"
    }
}