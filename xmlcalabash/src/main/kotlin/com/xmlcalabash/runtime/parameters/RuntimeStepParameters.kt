package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import net.sf.saxon.s9api.QName

open class RuntimeStepParameters(
    stepType: QName,
    stepName: String,
    location: Location,
    val inputs: Map<String, RuntimePort>,
    val outputs: Map<String, RuntimePort>,
    val options: Map<QName, RuntimeOption>
): StepParameters(stepType, stepName, location)