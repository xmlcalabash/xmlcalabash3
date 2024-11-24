package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.parameters.OptionStepParameters

class OptionExpressionStep(params: OptionStepParameters): ExpressionStep(params) {
    fun setExternalValue(value: XProcDocument) {
        override = value.with(stepConfig.checkType(null, value.value, params.asType, params.values))
    }
}