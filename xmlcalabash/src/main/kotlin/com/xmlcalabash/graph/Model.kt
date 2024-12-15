package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.StepDeclaration
import net.sf.saxon.s9api.QName

abstract class Model(val graph: Graph, val parent: Model?, val step: StepDeclaration) {
    val id = if (step.name.startsWith("!")) {
        "M${step.id}"
    } else {
        "${step.name}_${step.id}"
    }

    internal val saxonConfig = step.stepConfig.saxonConfig.newConfiguration(graph.environment)
    internal val inputs = mutableMapOf<String, ModelPort>()
    internal val outputs = mutableMapOf<String, ModelPort>()
    internal val options = mutableMapOf<QName, ModelOption>()

    internal abstract fun init()
}

