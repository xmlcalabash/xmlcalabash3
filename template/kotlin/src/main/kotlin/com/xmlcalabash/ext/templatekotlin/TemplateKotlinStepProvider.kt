package com.xmlcalabash.ext.templatekotlin

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class TemplateKotlinStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:template-kotlin"),
    { -> TemplateKotlin() })