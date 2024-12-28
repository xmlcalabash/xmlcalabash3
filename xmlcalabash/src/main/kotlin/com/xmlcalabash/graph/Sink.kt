package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.AtomicStepInstruction
import com.xmlcalabash.datamodel.XProcInstruction
import com.xmlcalabash.namespace.NsCx

class Sink(parent: XProcInstruction): AtomicStepInstruction(parent, NsCx.sink) {
}