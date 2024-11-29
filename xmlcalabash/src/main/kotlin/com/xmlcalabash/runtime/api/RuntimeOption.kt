package com.xmlcalabash.runtime.api

import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue

class RuntimeOption(val name: QName, val asType: SequenceType, val values: List<XdmAtomicValue>, val static: Boolean, val staticValue: XProcExpression? = null) {
}