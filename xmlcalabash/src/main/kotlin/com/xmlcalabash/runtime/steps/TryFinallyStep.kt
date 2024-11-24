package com.xmlcalabash.runtime.steps

import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel

open class TryFinallyStep(yconfig: RuntimeStepConfiguration, compound: CompoundStepModel): TryCatchStep(yconfig, compound) {
}