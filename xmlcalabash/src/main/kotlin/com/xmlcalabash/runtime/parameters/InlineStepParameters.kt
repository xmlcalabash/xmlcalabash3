package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import com.xmlcalabash.util.ValueTemplateFilter
import net.sf.saxon.s9api.QName

class InlineStepParameters(stepName: String,
                           location: Location,
                           inputs: Map<String, RuntimePort>,
                           outputs: Map<String, RuntimePort>,
                           options: Map<QName, RuntimeOption>,
                           val filter: ValueTemplateFilter,
                           val contentType: MediaType?,
                           val encoding: String?
): RuntimeStepParameters(NsCx.inline, stepName, location, inputs, outputs, options)