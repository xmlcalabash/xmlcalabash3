package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.api.RuntimeOption
import com.xmlcalabash.runtime.api.RuntimePort
import net.sf.saxon.s9api.QName

class DocumentStepParameters(stepName: String,
                             location: Location,
                             inputs: Map<String, RuntimePort>,
                             outputs: Map<String, RuntimePort>,
                             options: Map<QName, RuntimeOption>,
                             val contentType: MediaType?
): RuntimeStepParameters(NsCx.document, stepName, location, inputs, outputs, options)