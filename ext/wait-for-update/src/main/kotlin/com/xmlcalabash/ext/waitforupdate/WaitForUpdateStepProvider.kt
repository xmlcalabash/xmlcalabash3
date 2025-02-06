package com.xmlcalabash.ext.waitforupdate

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class WaitForUpdateStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:wait-for-update"),
    { -> WaitForUpdateStep() })