package com.xmlcalabash.ext.selenium

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class SeleniumStepProvider: SimpleStepProvider(QName(NsCx.namespace, "cx:selenium"), { -> SeleniumStep() })