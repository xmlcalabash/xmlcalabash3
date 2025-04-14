package com.xmlcalabash.config

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.XmlCalabashConfiguration
import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.api.Monitor
import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.exceptions.ErrorExplanation
import com.xmlcalabash.io.DocumentManager
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.AssertionsLevel
import net.sf.saxon.s9api.QName
import java.net.URI
import javax.activation.MimetypesFileTypeMap

interface XProcEnvironment {
    val episode: String
    val productName: String
    val productVersion: String
    val buildId: String
    val vendor: String
    val vendorUri: String
    val locale: String
    val version: String
    val xpathVersion: String

    val xmlCalabash: XmlCalabash
    val xmlCalabashConfig: XmlCalabashConfiguration
    val standardSteps: Map<QName, DeclareStepInstruction>
    fun stepProvider(params: StepParameters): () -> XProcStep
    val contentTypes: Map<String,String>

    val messagePrinter: MessagePrinter
    val monitors: MutableList<Monitor>
    val documentManager: DocumentManager
    val mimeTypes: MimetypesFileTypeMap
    val errorExplanation: ErrorExplanation
    val messageReporter: MessageReporter
    val proxies: Map<String, String>
    val assertions: AssertionsLevel

    fun atomicStepAvailable(type: QName): Boolean
    fun uniqueName(base: String): String
    fun uniqueUri(uri: String): URI
}