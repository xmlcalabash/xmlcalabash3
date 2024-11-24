package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.CompoundStepDeclaration

class PipelineModel internal constructor(graph: Graph, parent: Model?, step: CompoundStepDeclaration): CompoundModel(graph, parent, step) {
}