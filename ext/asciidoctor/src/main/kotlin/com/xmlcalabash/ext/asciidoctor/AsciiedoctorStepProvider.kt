package com.xmlcalabash.ext.asciidoctor

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class AsciiedoctorStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:asciidoctor"),
    { -> AsciidoctorStep() })
