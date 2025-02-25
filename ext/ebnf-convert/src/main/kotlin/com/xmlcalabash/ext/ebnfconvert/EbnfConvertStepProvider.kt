package com.xmlcalabash.ext.ebnfconvert

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class EbnfConvertStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:ebnf-convert"),
    { -> EbnfConvertStep() })