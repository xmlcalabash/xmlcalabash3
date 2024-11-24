package com.xmlcalabash.runtime

import net.sf.saxon.s9api.QName

class RuntimeOptionStep(pipelineConfig: XProcRuntime): RuntimeAtomicStep(pipelineConfig) {
    internal lateinit var optionName: QName
}