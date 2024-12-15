package com.xmlcalabash.datamodel

// The standard step library. There should only be one instance in any given pipeline configuration

class StandardLibraryInstruction(builder: PipelineBuilder, stepConfig: InstructionConfiguration): LibraryInstruction(stepConfig) {
    init {
        this.builder = builder
    }
}