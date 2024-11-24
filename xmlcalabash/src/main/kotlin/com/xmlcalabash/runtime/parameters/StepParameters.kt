package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.datamodel.Location
import net.sf.saxon.s9api.QName

open class StepParameters(val stepType: QName, val stepName: String = "", val location: Location = Location.NULL) {
}