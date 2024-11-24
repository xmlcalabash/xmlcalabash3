package com.xmlcalabash.exceptions

import net.sf.saxon.s9api.QName

class ErrorStackFrame(val stepType: QName, val stepName: String) {
    override fun toString() = "${stepType} / ${stepName}"
}