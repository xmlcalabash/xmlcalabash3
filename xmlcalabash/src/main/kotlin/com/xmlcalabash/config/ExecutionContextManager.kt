package com.xmlcalabash.config

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcExecutionContext
import com.xmlcalabash.runtime.XProcStepConfiguration

interface ExecutionContextManager {
    fun newExecutionContext(stepConfig: XProcStepConfiguration): XProcExecutionContext
    fun getExecutionContext(): XProcExecutionContext
    fun setExecutionContext(dynamicContext: XProcExecutionContext)
    fun releaseExecutionContext()
    fun addProperties(doc: XProcDocument?)
    fun removeProperties(doc: XProcDocument?)
}