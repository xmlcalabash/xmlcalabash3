package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import net.sf.saxon.s9api.QName

open class ViewportStepParameters(
    stepType: QName,
    stepName: String,
    location: Location,
    inputManifold: Map<String, RuntimePort>,
    outputManifold: Map<String, RuntimePort>,
    optionManifold: Map<QName, RuntimeOption>,
    val match: String
): RuntimeStepParameters(stepType, stepName, location, inputManifold, outputManifold, optionManifold)