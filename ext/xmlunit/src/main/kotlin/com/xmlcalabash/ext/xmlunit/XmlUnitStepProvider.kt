package com.xmlcalabash.ext.xmlunit

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class XmlUnitStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:xmlunit"),
    { -> XmlUnitStep() })