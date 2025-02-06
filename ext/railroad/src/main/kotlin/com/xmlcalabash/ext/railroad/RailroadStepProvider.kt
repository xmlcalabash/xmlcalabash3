package com.xmlcalabash.ext.rr

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class RailroadStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:railroad"),
    { -> RailroadStep() })