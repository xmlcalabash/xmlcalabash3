package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

open class InvisibleXmlImpl(val stepConfig: XProcStepConfiguration, val prefer: String) {
    companion object {
        private val nineml = mutableListOf<InvisibleXmlImpl?>()
        private val blitz = mutableListOf<InvisibleXmlImpl?>()
        private var loggedNineML = false
        private var loggedMarkupBlitz = false
    }

    open fun parse(grammar: String?, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        var tried = ""
        try {
            when (prefer) {
                "nineml" -> {
                    tried = "nineml"
                    val impl = loadNineML()
                    if (impl != null) {
                        if (!loggedNineML || loggedMarkupBlitz) {
                            stepConfig.debug { "Using NineML for p:invisible-xml"}
                            loggedNineML = true
                        }
                        return impl.parse(grammar, input, failOnError, parameters)
                    }
                }
                "blitz", "markup-blitz" -> {
                    tried = "markup-blitz"
                    val impl = loadMarkupBlitz()
                    if (impl != null) {
                        if (!loggedMarkupBlitz || loggedNineML) {
                            stepConfig.debug { "Using Markup Blitz for p:invisible-xml"}
                            loggedMarkupBlitz = true
                        }
                        return impl.parse(grammar, input, failOnError, parameters)
                    }
                }
                else -> {
                    throw stepConfig.exception(XProcError.xdStepFailed("Unknown Invisible XML implementation: ${prefer}"))
                }
            }
        } catch (ex: Throwable) { // Throwable to catch NoClassDefFoundException...
            if (ex is XProcException) {
                throw ex
            }
            if (tried == "nineml") {
                stepConfig.warn { "Failed to load NineML for Invisible XML parsing"}
            } else {
                stepConfig.warn { "Failed to load Markup Blitz for Invisible XML parsing"}
            }
        }

        // Try the other one
        try {
            if (tried == "nineml") {
                val impl = loadMarkupBlitz()
                if (impl != null) {
                    if (!loggedMarkupBlitz || loggedNineML) {
                        stepConfig.debug { "Using Markup Blitz for p:invisible-xml"}
                        loggedMarkupBlitz = true
                    }
                    return impl.parse(grammar, input, failOnError, parameters)
                }
            } else {
                val impl = loadNineML()
                if (impl != null) {
                    if (!loggedNineML || loggedMarkupBlitz) {
                        stepConfig.debug { "Using NineML for p:invisible-xml"}
                        loggedNineML = true
                    }
                    return impl.parse(grammar, input, failOnError, parameters)
                }
            }
        } catch (ex: Throwable) {
            if (ex is XProcException) {
                throw ex
            }
        }

        throw stepConfig.exception(XProcError.xdStepFailed("No Invisible XML implementation available"))
    }

    private fun loadNineML(): InvisibleXmlImpl? {
        if (nineml.isEmpty()) {
            val impl = InvisibleXmlNineML(stepConfig)
            nineml.add(impl)
        }
        return nineml.first()
    }

    private fun loadMarkupBlitz(): InvisibleXmlImpl? {
        if (blitz.isEmpty()) {
            val impl = InvisibleXmlMarkupBlitz(stepConfig)
            blitz.add(impl)
        }
        return blitz.first()
    }
}