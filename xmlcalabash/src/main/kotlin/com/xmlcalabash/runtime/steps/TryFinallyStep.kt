package com.xmlcalabash.runtime.steps

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class TryFinallyStep(config: XProcStepConfiguration, compound: CompoundStepModel): TryCatchStep(config, compound) {
}