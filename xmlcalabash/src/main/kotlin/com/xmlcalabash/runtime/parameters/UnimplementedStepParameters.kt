package com.xmlcalabash.runtime.parameters

import com.xmlcalabash.namespace.NsCx
import net.sf.saxon.s9api.QName

class UnimplementedStepParameters(val unimplemented: QName): StepParameters(NsCx.unimplemented)