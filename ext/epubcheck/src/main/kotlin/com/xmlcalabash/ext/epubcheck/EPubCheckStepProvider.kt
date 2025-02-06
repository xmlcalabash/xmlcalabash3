package com.xmlcalabash.ext.epubcheck

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class EPubCheckStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:epubcheck"),
    { -> EPubCheckStep() })
