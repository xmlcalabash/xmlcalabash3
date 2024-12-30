package com.xmlcalabash.xvrl

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder

class XvrlText(stepConfiguration: XProcStepConfiguration, val text: String): XvrlElement(stepConfiguration) {
    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addText(text)
    }
}