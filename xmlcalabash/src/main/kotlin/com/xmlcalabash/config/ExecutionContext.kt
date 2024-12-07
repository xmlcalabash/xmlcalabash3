package com.xmlcalabash.config

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.runtime.XProcExecutionContext
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.MessageReporter
import net.sf.saxon.s9api.QName
import java.net.URI
import javax.activation.MimetypesFileTypeMap

interface ExecutionContext {
    val episode: String
    val locale: String
    val productName: String
    val productVersion: String
    val gitHash: String
    val vendor: String
    val vendorUri: String
    val version: String
    val xpathVersion: String

    val documentManager: DocumentManager
    val mimeTypes: MimetypesFileTypeMap
    val errorExplanation: ErrorExplanation
    val messageReporter: MessageReporter
    val proxies: Map<String, String>

    fun stepProvider(params: StepParameters): () -> XProcStep
    fun atomicStepAvailable(type: QName): Boolean
    fun uniqueUri(base: String): URI

    fun newExecutionContext(stepConfig: XProcStepConfiguration): XProcExecutionContext
    fun getExecutionContext(): XProcExecutionContext
    fun setExecutionContext(dynamicContext: XProcExecutionContext)
    fun releaseExecutionContext()
    fun addProperties(doc: XProcDocument?)
    fun removeProperties(doc: XProcDocument?)
}