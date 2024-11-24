package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsP

class GroupInstruction internal constructor(parent: XProcInstruction): CompoundStepDeclaration(parent, parent.stepConfig.copy(), NsP.group) {
    override val contentModel = anySteps + mapOf(NsP.withInput to '0', NsP.output to '*')
}