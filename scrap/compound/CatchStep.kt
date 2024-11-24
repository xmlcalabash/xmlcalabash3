package com.xmlcalabash.steps.compound

import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.runtime.RuntimeCompoundStep
import net.sf.saxon.s9api.QName

class CatchStep(pipelineConfig: XProcRuntime, val codes: List<QName>): RuntimeCompoundStep(pipelineConfig) {
}