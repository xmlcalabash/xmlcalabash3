package com.xmlcalabash.ext.markupblitz

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class MarkupBlitzStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:markup-blitz"),
    { -> MarkupBlitzStep() })