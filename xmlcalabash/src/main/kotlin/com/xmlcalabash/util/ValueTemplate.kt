package com.xmlcalabash.util

data class ValueTemplate(val value: List<String>) {
    fun expressions(): List<String> {
        val expr = mutableListOf<String>()
        for (index in value.indices) {
            if (index % 2 == 1) {
                expr.add(value[index])
            }
        }
        return expr
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (index in value.indices) {
            if (index % 2 == 0) {
                sb.append(value[index])
            } else {
                sb.append("{").append(value[index]).append("}")
            }
        }
        return sb.toString()
    }
}