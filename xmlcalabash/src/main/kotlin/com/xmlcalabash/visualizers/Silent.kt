package com.xmlcalabash.visualizers

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.api.Monitor
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.Consumer
import org.apache.logging.log4j.kotlin.logger
import java.util.Stack

open class Silent(options: Map<String,String>): AbstractVisualizer(emptyMap()) {
    init {
        if (options.isNotEmpty()) {
            logger.warn("The silent visualizer accepts no options")
        }
    }
    override fun showStart(step: AbstractStep, depth: Int) {
        // nop
    }

    override fun showEnd(step: AbstractStep, depth: Int) {
        // nop
    }

    override fun showDocument(step: AbstractStep, port: String, depth: Int, document: XProcDocument) {
        // nop
    }
}