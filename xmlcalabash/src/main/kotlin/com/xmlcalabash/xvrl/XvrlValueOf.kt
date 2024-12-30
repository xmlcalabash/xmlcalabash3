package com.xmlcalabash.xvrl

import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName

class XvrlValueOf private constructor(stepConfiguration: XProcStepConfiguration, val name: QName): XvrlElement(stepConfiguration) {
}