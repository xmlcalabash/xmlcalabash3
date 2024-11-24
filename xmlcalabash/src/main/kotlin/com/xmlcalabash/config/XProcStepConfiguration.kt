package com.xmlcalabash.config

import com.xmlcalabash.documents.DocumentContext
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.ValueConverter
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmValue

interface XProcStepConfiguration: DocumentContext, ValueConverter, ExecutionContext {
    val saxonConfig: SaxonConfiguration

    fun stepAvailable(name: QName): Boolean
    fun parseQName(name: String): QName
    fun checkType(varName: QName?, value: XdmValue, sequenceType: SequenceType?, values: List<XdmAtomicValue>): XdmValue
    fun forceQNameKeys(inputMap: MapItem): XdmMap
    fun forceQNameKeys(inputMap: XdmMap): XdmMap
    fun exception(error: XProcError): XProcException
    fun exception(error: XProcError, cause: Throwable): XProcException
}