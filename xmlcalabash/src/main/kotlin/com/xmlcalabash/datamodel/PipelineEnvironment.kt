package com.xmlcalabash.datamodel

import com.xmlcalabash.config.CommonEnvironment
import com.xmlcalabash.config.ExecutionContextManager
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.debugger.Debugger
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.tracing.TraceListener
import com.xmlcalabash.util.MessageReporter
import net.sf.saxon.s9api.QName
import java.net.URI
import javax.activation.MimetypesFileTypeMap

interface PipelineEnvironment: ExecutionContextManager {
    val commonEnvironment: CommonEnvironment
    val xmlCalabash: XmlCalabash
    val episode: String
    val locale: String
    val productName: String
    val productVersion: String
    val gitHash: String
    val vendor: String
    val vendorUri: String
    val version: String
    val xpathVersion: String
    var uniqueInlineUris: Boolean

    val debugger: Debugger
    val traceListener: TraceListener
    val documentManager: DocumentManager
    val mimeTypes: MimetypesFileTypeMap
    val errorExplanation: ErrorExplanation
    val messageReporter: MessageReporter
    val proxies: Map<String, String>

    val nextId: String

    fun uniqueUri(base: String): URI
    val standardSteps: Map<QName, DeclareStepInstruction>
}