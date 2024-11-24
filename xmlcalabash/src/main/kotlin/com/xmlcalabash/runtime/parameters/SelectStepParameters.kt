package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import net.sf.saxon.s9api.QName

class SelectStepParameters(stepName: String,
                           location: Location,
                           inputs: Map<String, RuntimePort>,
                           outputs: Map<String, RuntimePort>,
                           options: Map<QName, RuntimeOption>,
                           val select: XProcExpression
): RuntimeStepParameters(NsCx.select, stepName, location, inputs, outputs, options)