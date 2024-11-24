package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.ConnectionInstruction
import com.xmlcalabash.datamodel.InputBindingInstruction

class ModelInputPort(parent: Model, input: InputBindingInstruction): ModelPort(parent, input) {
    val defaultBindings: List<ConnectionInstruction> = input.defaultBindings
}