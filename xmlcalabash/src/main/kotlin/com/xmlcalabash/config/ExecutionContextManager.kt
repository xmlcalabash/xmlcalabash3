package com.xmlcalabash.config

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcExecutionContext
import net.sf.saxon.s9api.XdmValue
import java.util.*

interface ExecutionContextManager {
    fun newExecutionContext(stepConfig: StepConfiguration): XProcExecutionContext
    fun getExecutionContext(): XProcExecutionContext
    fun setExecutionContext(dynamicContext: XProcExecutionContext)
    fun releaseExecutionContext()
    fun preserveExecutionContext(): Stack<XProcExecutionContext>
    fun restoreExecutionContext(contextStack: Stack<XProcExecutionContext>)
    fun addProperties(doc: XProcDocument?)
    fun removeProperties(doc: XProcDocument?)
}