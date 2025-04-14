package com.xmlcalabash.config

import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.util.AssertionsLevel
import javax.activation.MimetypesFileTypeMap

interface PipelineConfiguration {
    val messagePrinter: MessagePrinter
    //val monitors: List<Monitor>
    //val documentManager: DocumentManager
    val mimeTypes: MimetypesFileTypeMap
    //val errorExplanation: ErrorExplanation
    //val messageReporter: MessageReporter
    val proxies: Map<String, String>
    val assertions: AssertionsLevel
}