package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsP

class ForEachInstruction(parent: XProcInstruction): CompoundLoopDeclaration(parent, NsP.forEach) {
    override val contentModel = anySteps + mapOf(NsP.withInput to '1', NsP.output to '*')
}