package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.AtomicStepInstruction
import com.xmlcalabash.datamodel.XProcInstruction
import com.xmlcalabash.namespace.NsCx

class Splitter(parent: XProcInstruction): AtomicStepInstruction(parent, NsCx.splitter) {
}