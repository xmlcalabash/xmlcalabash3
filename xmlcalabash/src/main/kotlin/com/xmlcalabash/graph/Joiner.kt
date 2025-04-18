package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.AtomicStepInstruction
import com.xmlcalabash.datamodel.XProcInstruction
import com.xmlcalabash.namespace.NsCx

class Joiner(parent: XProcInstruction): AtomicStepInstruction(parent, NsCx.joiner) {
}