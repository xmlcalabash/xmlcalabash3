package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import net.sf.saxon.s9api.QName

open class RunStepStepParameters(
    stepType: QName,
    stepName: String,
    location: Location,
    inputs: Map<String, RuntimePort>,
    outputs: Map<String, RuntimePort>,
    options: Map<QName, RuntimeOption>,
    val primaryInput: String?,
    val primaryOutput: String?
): RuntimeStepParameters(stepType, stepName, location, inputs, outputs, options)