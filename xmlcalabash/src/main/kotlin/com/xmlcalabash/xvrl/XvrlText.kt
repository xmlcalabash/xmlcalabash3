package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder

class XvrlText(stepConfiguration: StepConfiguration, val text: String): XvrlElement(stepConfiguration) {
    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addText(text)
    }
}