package com.xmlcalabash.ext.xpath

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class XPathStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:xpath"),
    { -> XPathStep() })