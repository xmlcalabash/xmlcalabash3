package com.xmlcalabash.ext.uniqueid

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class UniqueIdStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:unique-id"),
    { -> UniqueIdStep() })