package com.xmlcalabash.runtime.steps

import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.runtime.parameters.TryCatchStepParameters

open class TryCatchStep(yconfig: RuntimeStepConfiguration, compound: CompoundStepModel): GroupStep(yconfig, compound) {
    internal val codes = (params as TryCatchStepParameters).codes
}