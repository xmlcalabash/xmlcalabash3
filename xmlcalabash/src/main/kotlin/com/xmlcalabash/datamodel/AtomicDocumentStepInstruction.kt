package com.xmlcalabash.datamodel

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsCx

class AtomicDocumentStepInstruction(parent: XProcInstruction): AtomicStepInstruction(parent, NsCx.document) {
    var contentType: MediaType? = null
    lateinit var documentProperties: XProcExpression
    lateinit var parameters: XProcExpression

    init {
        name = stepConfig.uniqueName("!${instructionType.localName}")
    }
}