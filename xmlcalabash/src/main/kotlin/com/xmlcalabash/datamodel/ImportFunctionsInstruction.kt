package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.functions.FunctionLibrary
import net.sf.saxon.om.NamespaceUri
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.net.URI
import javax.xml.transform.sax.SAXSource

class ImportFunctionsInstruction(parent: XProcInstruction?, stepConfig: StepConfiguration, val inputHref: URI): XProcInstruction(parent, stepConfig, NsP.importFunctions) {
    val href: URI
        get() {
            val uri = stepConfig.baseUri
            if (uri == null) {
                return inputHref
            }
            return uri.resolve(inputHref)
        }

    var contentType: MediaType? = null
        set(value) {
            checkOpen()
            field = value
        }

    var namespace: NamespaceUri? = null
        set(value) {
            checkOpen()
            field = value
        }

    private var _functionLibrary: FunctionLibrary? = null
    val functionLibrary: FunctionLibrary?
        get() = _functionLibrary

    fun prefetch(): FunctionLibrary? {
        if (stepConfig.processor.underlyingConfiguration.editionCode != "EE") {
            logger.debug { "Saxon EE required for p:import-functions" }
            return null
        }

        val ctype = if (contentType == null) {
            stepConfig.mimeTypes.getContentType(href.toString())
        } else {
            contentType.toString()
        }

        val ns = namespace?.toString()

        try {
            val url = href.toURL()
            val conn = url.openConnection()

            _functionLibrary = if (ctype.contains("xsl")) {
                val stylesheet = SAXSource(InputSource(conn.getInputStream()))
                val compiler = stepConfig.processor.newXsltCompiler()
                val exec = compiler.compile(stylesheet)
                val ps = exec.underlyingCompiledStylesheet
                val loaded = ps.functionLibrary
                loaded
            } else {
                if (ns == null) {
                    logger.debug { "Cannot load XQuery functions without a specified namespace" }
                    return null
                }
                val compiler = stepConfig.processor.newXQueryCompiler()
                val context = compiler.underlyingStaticContext
                context.compileLibrary(conn.getInputStream(), "utf-8")
                val expression = context.compileQuery("import module namespace f='${ns}';.")
                val module = expression.mainModule
                module.globalFunctionLibrary
            }

            stepConfig.saxonConfig.addFunctionLibrary(href, functionLibrary!!)
        } catch (ex: Exception) {
            throw XProcError.xsImportFunctionsUnloadable(href).exception()
        }

        return functionLibrary
    }
}