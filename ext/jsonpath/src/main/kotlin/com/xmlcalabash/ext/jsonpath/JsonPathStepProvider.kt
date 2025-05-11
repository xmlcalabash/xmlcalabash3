package com.xmlcalabash.ext.jsonpath

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class JsonPathStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:jsonpath"),
    { -> JsonPathStep() })