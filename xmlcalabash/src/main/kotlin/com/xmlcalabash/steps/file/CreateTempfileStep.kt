package com.xmlcalabash.steps.file

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.Urify
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CreateTempfileStep(): FileStep(NsP.fileCreateTempfile) {
    override fun run() {
        super.run()

        var exception: Exception? = null
        val failOnError = booleanBinding(Ns.failOnError) ?: true
        val deleteOnExit = booleanBinding(Ns.deleteOnExit) ?: false
        var tempfile: Path? = null

        val href = try {
            uriBinding(Ns.href)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdInvalidUri(options[Ns.href].toString()), ex)
        }

        try {
            val dir: Path?
            if (href != null) {
                if (href.scheme != "file") {
                    throw stepConfig.exception(XProcError.xcUnsupportedFileCreateTempfileScheme(href.scheme))
                }

                dir = Paths.get(UriUtils.path(href))

                if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                    if (failOnError) {
                        throw stepConfig.exception(XProcError.xdDoesNotExist(dir.toString(), "path is not a directory"))
                    } else {
                        val err = errorDocument(href, NsErr.xd(11))
                        receiver.output("result", XProcDocument.ofXml(err, stepConfig))
                        return
                    }
                }
            } else {
                val tdir = System.getProperty("java.io.tmpdir")
                if (tdir != null) {
                    dir = Paths.get(tdir)
                } else {
                    // I give up, use the current directory
                    dir = Paths.get(".")
                }
            }

            val prefix = stringBinding(Ns.prefix)
            val suffix = stringBinding(Ns.suffix)
            tempfile = Files.createTempFile(dir!!, prefix, suffix)
            if (deleteOnExit) {
                tempfile.toFile().deleteOnExit()
            }
        } catch (ex: XProcException) {
            if (failOnError) {
                throw ex
            }
            exception = ex
        } catch (ex: IOException) {
            if (failOnError) {
                throw stepConfig.exception(XProcError.xcTemporaryFileCreateFailed(), ex)
            }
            exception = ex
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)

        if (exception != null) {
            errorFromException(builder, NsErr.xc(116), exception)
        } else {
            builder.addStartElement(NsC.result)
            builder.addText(tempfile!!.toUri().toString())
            builder.addEndElement()
        }

        builder.endDocument()
        receiver.output("result", XProcDocument(builder.result, stepConfig))
    }
}