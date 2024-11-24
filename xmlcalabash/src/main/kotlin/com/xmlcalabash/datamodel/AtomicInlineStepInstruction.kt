package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.ValueTemplateFilter

class AtomicInlineStepInstruction(parent: InlineInstruction, val filter: ValueTemplateFilter): AtomicStepInstruction(parent, NsCx.inline) {
    var contentType = parent.contentType
    var encoding = parent.encoding
    var documentProperties = parent.documentProperties

    init {
        name = "!${instructionType.localName}_${stepConfig.nextId}"
    }
}