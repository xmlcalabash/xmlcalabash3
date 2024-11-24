package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.datamodel.AtomicExpressionStepInstruction
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import net.sf.saxon.s9api.QName

open class ExpressionStepParameters(
    stepName: String,
    location: Location,
    inputs: Map<String, RuntimePort>,
    outputs: Map<String, RuntimePort>,
    options: Map<QName, RuntimeOption>,
    step: AtomicExpressionStepInstruction,
    stepType: QName = NsCx.expression
): RuntimeStepParameters(stepType, stepName, location, inputs, outputs, options) {
    val expression = step.expression
    val asType = step.expression.asType
    val values = step.expression.values
    val collection = step.expression.collection
    val extensionAttr = step.extensionAttributes
}
