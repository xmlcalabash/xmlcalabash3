package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.SaxonErrorReporter
import net.sf.saxon.functions.ExecutableFunctionLibrary
import net.sf.saxon.functions.FunctionLibrary
import net.sf.saxon.functions.FunctionLibraryList
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.query.XQueryFunctionLibrary
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XPathCompiler
import net.sf.saxon.trans.Visibility
import net.sf.saxon.trans.XPathException
import org.xml.sax.InputSource
import java.io.InputStream
import java.net.URI
import java.net.URLConnection
import javax.xml.transform.sax.SAXSource

class ImportFunctionsInstruction(parent: XProcInstruction?, stepConfig: InstructionConfiguration, val inputHref: URI): XProcInstruction(parent, stepConfig, NsP.importFunctions) {
    companion object {
        private val XQueryNs = NamespaceUri.of("http://www.w3.org/2012/xquery")
    }

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

    var namespace: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    val errorReporter = SaxonErrorReporter(stepConfig)

    private var _functionLibrary: XProcFunctionLibrary? = null
    val functionLibrary: XProcFunctionLibrary?
        get() = _functionLibrary

    fun prefetch(): XProcFunctionLibrary? {
        if (stepConfig.saxonConfig.processor.underlyingConfiguration.editionCode != "EE") {
            stepConfig.warn { "Saxon EE required for p:import-functions" }
            return null
        }

        val ctype = if (contentType == null) {
            MediaType.parse(stepConfig.documentManager.mimetypesFileTypeMap.getContentType(href.toString()))
        } else {
            contentType!!
        }

        try {
            val url = href.toURL()
            val conn = url.openConnection()

            _functionLibrary = when (ctype) {
                MediaType.XSLT -> loadXsltLibrary(conn)
                MediaType.XQUERY -> loadXQueryLibrary(conn)
                else -> {
                    throw stepConfig.exception(XProcError.xsUnknownFunctionMediaType(ctype.toString()))
                }
            }

            if (functionLibrary != null) {
                stepConfig.saxonConfig.addFunctionLibrary(href, functionLibrary!!)
            }

            return functionLibrary
        } catch (ex: Exception) {
            if (ex is XProcException) {
                throw ex
            }
            throw stepConfig.exception(XProcError.xsImportFunctionsUnloadable(href), ex)
        }
    }

    private fun loadXsltLibrary(conn: URLConnection): XProcFunctionLibrary? {
        val stylesheet = SAXSource(InputSource(conn.getInputStream()))
        val compiler = stepConfig.saxonConfig.processor.newXsltCompiler()
        compiler.isSchemaAware = stepConfig.saxonConfig.processor.isSchemaAware
        compiler.errorReporter = errorReporter
        val exec = compiler.compile(stylesheet)
        val ps = exec.underlyingCompiledStylesheet
        val loaded = ps.functionLibrary

        // Find the one we just loaded. This is a bit of a hack...
        var allFunctionsLib: ExecutableFunctionLibrary? = null
        for (lib in loaded.libraryList.filter { it is ExecutableFunctionLibrary }) {
            val elib = lib as ExecutableFunctionLibrary
            if (!lib.allFunctions.toList().isEmpty()) {
                allFunctionsLib = elib
                break
            }
        }

        if (allFunctionsLib != null) {
            val functionLibrary = ExecutableFunctionLibrary(stepConfig.saxonConfig.configuration)
            val exposedNames = mutableMapOf<QName, Int>()
            val namespaceSet = mutableSetOf<NamespaceUri>()
            if (namespace != null) {
                for (ns in namespace!!.split("\\s+")) {
                    namespaceSet.add(NamespaceUri.of(ns))
                }
            }
            for (function in allFunctionsLib.allFunctions) {
                if (namespaceSet.isEmpty() || function.functionName.namespaceUri in namespaceSet) {
                    if (function.declaredVisibility != Visibility.PRIVATE) {
                        exposedNames.put(stepConfig.typeUtils.QNameFromStructuredQName(function.functionName),
                            function.numberOfParameters)
                        functionLibrary.addFunction(function)
                    }
                }
            }

            return XProcFunctionLibrary(exposedNames, functionLibrary)
        } else {
            return null
        }
    }

    private fun loadXQueryLibrary(conn: URLConnection): XProcFunctionLibrary? {
        val compiler = stepConfig.processor.newXQueryCompiler()
        compiler.isSchemaAware = stepConfig.processor.isSchemaAware
        compiler.errorReporter = errorReporter
        val context = compiler.underlyingStaticContext

        val knownLibraries = mutableSetOf<NamespaceUri>()
        for (library in context.compiledLibraries) {
            knownLibraries.add(library.moduleNamespace)
        }

        try {
            context.compileLibrary(conn.inputStream, "utf-8")

            var newns: NamespaceUri? = null
            for (library in context.compiledLibraries) {
                if (library.moduleNamespace !in knownLibraries) {
                    newns = library.moduleNamespace
                    break
                }
            }

            if (newns == null) {
                throw stepConfig.exception(XProcError.xsImportFunctionsUnloadable(href))
            }

            if (namespace != null) {
                var useThisNamespace = false
                for (ns in namespace!!.split("\\s")) {
                    if (newns == NamespaceUri.of(ns)) {
                        useThisNamespace = true
                        break
                    }
                }

                // Nope. Nothing here for us.
                if (!useThisNamespace) {
                    return null
                }
            }

            val expression = context.compileQuery("import module namespace f='${newns}';.")
            val module = expression.mainModule
            return filterXQueryFunctionLibrary(module.globalFunctionLibrary)
        } catch (ex: XPathException) {
            val message = ex.message ?: ""
            val notQueryModule =
                message.contains("imported for module") && message.contains("is not a valid XQuery library module")
            if (!notQueryModule) {
                throw ex
            }
        }

        // Maybe it's not a library module, maybe it's a main module?
        // Re-open the stream...
        val url = href.toURL()
        val conn = url.openConnection()

        return loadXQueryMainModule(conn.inputStream)
    }

    private fun loadXQueryMainModule(bufstream: InputStream): XProcFunctionLibrary? {
        val compiler = stepConfig.processor.newXQueryCompiler()
        compiler.isSchemaAware = stepConfig.processor.isSchemaAware
        compiler.errorReporter = errorReporter
        compiler.setSchemaAware(stepConfig.processor.isSchemaAware)
        val exec = compiler.compile(bufstream)
        val loaded = exec.underlyingCompiledQuery.executable.functionLibrary

        // Find the one we just loaded. This is a bit of a hack...
        var allFunctionsLib: XQueryFunctionLibrary? = null
        for (lib in loaded.libraryList.filter { it is XQueryFunctionLibrary }) {
            val elib = lib as XQueryFunctionLibrary
            var empty = true
            for (item in lib.functionDefinitions) {
                empty = false
                break
            }
            if (!empty) {
                allFunctionsLib = elib
                break
            }
        }

        if (allFunctionsLib == null) {
            return null
        }

        val functionLibrary = XQueryFunctionLibrary(stepConfig.saxonConfig.configuration)
        val namespaceSet = mutableSetOf<NamespaceUri>()
        if (namespace != null) {
            for (ns in namespace!!.split("\\s+")) {
                namespaceSet.add(NamespaceUri.of(ns))
            }
        }
        for (function in allFunctionsLib.functionDefinitions) {
            if (namespaceSet.isEmpty() || function.functionName.namespaceUri in namespaceSet) {
                functionLibrary.declareFunction(function)
            }
        }

        return filterXQueryFunctionLibrary(functionLibrary)
    }

    private fun filterXQueryFunctionLibrary(library: XQueryFunctionLibrary): XProcFunctionLibrary {
        val functionLibrary = XQueryFunctionLibrary(library.configuration)
        val exposedNames = mutableMapOf<QName, Int>()
        for (function in library.functionDefinitions) {
            var private = false
            for (annotation in function.annotations) {
                if (annotation.annotationQName.namespaceUri == XQueryNs
                    && annotation.annotationQName.localPart == "private") {
                    private = true
                    break
                }
            }
            if (!private) {
                exposedNames.put(stepConfig.typeUtils.QNameFromStructuredQName (function.functionName),
                    function.numberOfParameters)
                functionLibrary.declareFunction(function)
            }
        }
        return XProcFunctionLibrary(exposedNames, functionLibrary)
    }
}