package com.xmlcalabash.steps.extension

import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName

abstract class AbstractRdfStep(): AbstractAtomicStep() {
    companion object {
        val _graph = QName("graph")
        val _language = QName("language")
    }
}