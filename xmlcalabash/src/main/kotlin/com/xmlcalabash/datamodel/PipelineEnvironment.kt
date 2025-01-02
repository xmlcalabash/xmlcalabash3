package com.xmlcalabash.datamodel

import com.xmlcalabash.config.CommonEnvironment
import com.xmlcalabash.config.ExecutionContextManager
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.api.Monitor
import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.util.AssertionsLevel
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
    val buildId: String
    val vendor: String
    val vendorUri: String
    val version: String
    val xpathVersion: String
    var uniqueInlineUris: Boolean

    val monitors: List<Monitor>
    val documentManager: DocumentManager
    val mimeTypes: MimetypesFileTypeMap
    val errorExplanation: ErrorExplanation
    val messageReporter: MessageReporter
    val proxies: Map<String, String>
    val assertions: AssertionsLevel

    val nextId: String
    fun uniqueName(base: String): String

    fun uniqueUri(base: String): URI
    val standardSteps: Map<QName, DeclareStepInstruction>
}