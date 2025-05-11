package com.xmlcalabash.ext.find

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class FindStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:find"),
    { -> FindStep() })