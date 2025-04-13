package com.xmlcalabash.graph

import com.xmlcalabash.config.XProcEnvironment
import com.xmlcalabash.datamodel.CompileEnvironment

class GraphEnvironment(val environment: CompileEnvironment): XProcEnvironment by environment {
    override fun uniqueName(base: String): String {
        val key = if (base.startsWith("!")) {
            base.substring(1)
        } else {
            base
        }

        if (key in environment.nameCounts) {
            val suffix = environment.nameCounts[key]!! + 1
            environment.nameCounts[key] = suffix
            return "${key}_${suffix}"
        }
        environment.nameCounts[key] = 1
        return key
    }
}