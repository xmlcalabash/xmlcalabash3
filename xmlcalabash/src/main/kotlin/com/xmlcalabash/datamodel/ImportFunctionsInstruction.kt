package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.functions.ExecutableFunctionLibrary
import net.sf.saxon.functions.FunctionLibrary
import net.sf.saxon.functions.FunctionLibraryList
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.query.XQueryFunctionLibrary
import net.sf.saxon.trans.XPathException
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.InputStream
import java.net.URI
import java.net.URLConnection
import javax.xml.transform.sax.SAXSource

class ImportFunctionsInstruction(parent: XProcInstruction?, stepConfig: InstructionConfiguration, val inputHref: URI): XProcInstruction(parent, stepConfig, NsP.importFunctions) {
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

    private var _functionLibrary: FunctionLibrary? = null
    val functionLibrary: FunctionLibrary?
        get() = _functionLibrary

    fun prefetch(): FunctionLibrary? {
        if (stepConfig.processor.underlyingConfiguration.editionCode != "EE") {
            logger.debug { "Saxon EE required for p:import-functions" }
            return null
        }

        val ctype = if (contentType == null) {
            MediaType.parse(stepConfig.environment.mimeTypes.getContentType(href.toString()))
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
                    throw XProcError.xsUnknownFunctionMediaType(ctype.toString()).exception()
                }
            }

            if (functionLibrary != null) {
                stepConfig.saxonConfig.addFunctionLibrary(href, functionLibrary!!)
            }

            return functionLibrary
        } catch (ex: Exception) {
            throw XProcError.xsImportFunctionsUnloadable(href).exception(ex)
        }
    }

    private fun loadXsltLibrary(conn: URLConnection): FunctionLibrary? {
        val stylesheet = SAXSource(InputSource(conn.getInputStream()))
        val compiler = stepConfig.processor.newXsltCompiler()
        val exec = compiler.compile(stylesheet)
        val ps = exec.underlyingCompiledStylesheet
        val loaded = ps.functionLibrary

        if (namespace == null) {
            return loaded
        }

        // Find the one we just loaded. This is a bit of a hack...
        var allFunctionsLib: ExecutableFunctionLibrary? = null
        for (lib in loaded.libraryList.filter { it is ExecutableFunctionLibrary }) {
            val elib = lib as ExecutableFunctionLibrary
            var empty = true
            for (item in lib.allFunctions) {
                empty = false
                break
            }
            if (!empty) {
                allFunctionsLib = elib
                break
            }
        }

        if (allFunctionsLib != null) {
            val functionLibrary = ExecutableFunctionLibrary(stepConfig.saxonConfig.configuration)
            val namespaceSet = mutableSetOf<NamespaceUri>()
            for (ns in namespace!!.split("\\s+")) {
                namespaceSet.add(NamespaceUri.of(ns))
            }
            for (function in allFunctionsLib.allFunctions) {
                if (function.functionName.namespaceUri in namespaceSet) {
                    functionLibrary.addFunction(function)
                }
            }

            val newList = FunctionLibraryList()
            for (lib in loaded.libraryList) {
                if (lib === allFunctionsLib) {
                    newList.addFunctionLibrary(functionLibrary)
                } else {
                    newList.addFunctionLibrary(lib)
                }
            }

            return newList
        } else {
            return loaded
        }
    }

    private fun loadXQueryLibrary(conn: URLConnection): FunctionLibrary? {
        val compiler = stepConfig.processor.newXQueryCompiler()
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
                throw XProcError.xsImportFunctionsUnloadable(href).exception()
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
            return module.globalFunctionLibrary
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

    private fun loadXQueryMainModule(bufstream: InputStream): FunctionLibrary? {
        val compiler = stepConfig.processor.newXQueryCompiler()
        compiler.setSchemaAware(false) // FIXME:
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

        if (allFunctionsLib != null) {
            if (namespace == null) {
                return allFunctionsLib
            }

            val functionLibrary = XQueryFunctionLibrary(stepConfig.saxonConfig.configuration)
            val namespaceSet = mutableSetOf<NamespaceUri>()
            for (ns in namespace!!.split("\\s+")) {
                namespaceSet.add(NamespaceUri.of(ns))
            }
            for (function in allFunctionsLib.functionDefinitions) {
                if (function.functionName.namespaceUri in namespaceSet) {
                    functionLibrary.declareFunction(function)
                }
            }

            val newList = FunctionLibraryList()
            for (lib in loaded.libraryList) {
                if (lib === allFunctionsLib) {
                    newList.addFunctionLibrary(functionLibrary)
                } else {
                    newList.addFunctionLibrary(lib)
                }
            }

            return newList
        }

        return null
    }
}