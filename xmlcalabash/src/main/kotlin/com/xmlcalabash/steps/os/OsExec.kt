package com.xmlcalabash.steps.os

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.Urify
import net.sf.saxon.s9api.SaxonApiException
import java.io.*
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths

class OsExec(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val documents = queues["source"]!!
        if (documents.size > 1) {
            throw stepConfig.exception(XProcError.xcOsExecMultipleInputs())
        }

        var command = stringBinding(Ns.command)!!
        val args = mutableListOf<String>()
        if (options[Ns.args] != null) {
            for (arg in options[Ns.args]!!.value) {
                args.add(arg.toString())
            }
        }
        var cwd = stringBinding(Ns.cwd)
        val resultContentType = MediaType.parse(stringBinding(Ns.resultContentType)!!)
        val errorContentType = MediaType.parse(stringBinding(Ns.errorContentType)!!)
        val pathSeparator = stringBinding(Ns.pathSeparator)
        val failureThreshold = integerBinding(Ns.failureThreshold)
        val serialization = qnameMapBinding(Ns.serialization)

        val resultCharset = defaultCharset(resultContentType, errorContentType)
        val errorCharset = defaultCharset(errorContentType, resultContentType)

        val slash = FileSystems.getDefault().getSeparator()
        if (pathSeparator != null) {
            if (pathSeparator.length > 1) {
                throw stepConfig.exception(XProcError.xcOsExecBadSeparator(pathSeparator))
            }
            command = command.replace(pathSeparator, slash)
            if (cwd != null) {
                cwd = cwd.replace(pathSeparator, slash)
            }
            val newList = mutableListOf<String>()
            for (arg in args) {
                newList.add(arg.replace(pathSeparator, slash))
            }
            args.clear()
            args.addAll(newList)
        }

        if (cwd != null) {
            cwd = Urify.urify(cwd)
            if (!cwd.startsWith("file:")) {
                throw stepConfig.exception(XProcError.xcOsExecBadCwd(cwd))
            }
            val path = Paths.get(UriUtils.path(URI(cwd)))
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                throw stepConfig.exception(XProcError.xcOsExecBadCwd(cwd))
            }
            cwd = path.toString()
        }

        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val showCmd = StringBuilder()
        val commandLine = mutableListOf<String>()
        var rc = 0

        showCmd.append(command)
        commandLine.add(command)

        for (arg in args) {
            showCmd.append(" ").append(arg)
            commandLine.add(arg)
        }

        val builder = ProcessBuilder(commandLine)
        if (cwd != null) {
            builder.directory(File(cwd))
        }

        stepConfig.debug { "OS exec: ${showCmd}" }
        stepConfig.debug { "OS exec charsets: result=${resultCharset}; error=${errorCharset}" }

        try {
            val process = builder.start()

            val stdoutReader = ProcessOutputReader(process.inputStream, stdout, resultCharset)
            val stderrReader = ProcessOutputReader(process.errorStream, stderr, errorCharset)

            val stdoutThread = Thread(stdoutReader)
            val stderrThread = Thread(stderrReader)

            stdoutThread.start()
            stderrThread.start()

            if (documents.isNotEmpty()) {
                // If the process doesn't care about input and finishes before we finish serializing, this
                // can cause an error.
                try {
                    val os = process.outputStream
                    DocumentWriter(documents.first(), os, serialization).write()
                    os.close()
                } catch (ex: SaxonApiException) {
                    var ignore = false
                    if (ex.cause != null) {
                        val cause = ex.cause!!
                        if (cause.cause is IOException) {
                            ignore = cause.cause!!.message!!.contains("Stream closed")
                        }
                    }
                    if (!ignore) {
                        throw ex
                    }
                }
            }

            rc = process.waitFor()
            stdoutThread.join()
            stderrThread.join()
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcOsExecFailed(), ex)
        }

        if (failureThreshold != null && failureThreshold < rc) {
            throw stepConfig.exception(XProcError.xcOsExecFailed(rc))
        }

        // Always get the result as a string so we can see if there *is* any result
        var text = stdout.toString()
        if (text.isNotEmpty()) {
            if (resultContentType.classification() == MediaClassification.TEXT) {
                val outputText = if (Urify.isWindows) {
                    text.replace("\r\n", "\n")
                } else {
                    text
                }
                receiver.output("result", XProcDocument.ofText(outputText, stepConfig, resultContentType, DocumentProperties()))
            } else {
                val outputLoader = DocumentLoader(stepConfig, null, DocumentProperties(), mapOf())
                val output = outputLoader.load(ByteArrayInputStream(text.toByteArray()), resultContentType)
                receiver.output("result", output)
            }
        }

        text = stderr.toString()
        if (text.isNotEmpty()) {
            if (resultContentType.classification() == MediaClassification.TEXT) {
                val outputText = if (Urify.isWindows) {
                    text.replace("\r\n", "\n")
                } else {
                    text
                }
                receiver.output("error", XProcDocument.ofText(outputText, stepConfig, errorContentType,DocumentProperties()))
            } else {
                val errorLoader = DocumentLoader(stepConfig, null, DocumentProperties(), mapOf())
                val error = errorLoader.load(ByteArrayInputStream(text.toByteArray()), errorContentType)
                receiver.output("error", error)
            }
        }

        val sbuilder = SaxonTreeBuilder(stepConfig)
        sbuilder.startDocument(null)
        sbuilder.addStartElement(NsC.result)
        sbuilder.addText(rc.toString())
        sbuilder.addEndElement()
        sbuilder.endDocument()
        receiver.output("exit-status", XProcDocument.ofXml(sbuilder.result, stepConfig))
    }

    private fun defaultCharset(type1: MediaType, type2: MediaType): Charset {
        val charset1 = type1.charset()
        if (charset1 != null) {
            return charset1
        }
        val charset2 = type2.charset()
        if (charset2 != null) {
            return charset2
        }
        return Charset.defaultCharset()
    }

    inner class ProcessOutputReader(val stream: InputStream, val buffer: StringBuilder, val charset: Charset): Runnable {
        override fun run() {
            val reader = InputStreamReader(stream, charset)
            val buf = CharArray(4096)
            var len = reader.read(buf)
            while (len >= 0) {
                if (len == 0) {
                    Thread.sleep(250)
                } else {
                    buffer.append(buf, 0, len)
                }
                len = reader.read(buf)
            }
        }
    }

    override fun toString(): String = "p:os-exec"
}