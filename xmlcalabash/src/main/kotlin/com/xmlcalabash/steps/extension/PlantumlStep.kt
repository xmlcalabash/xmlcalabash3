package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.ByteArrayOutputStream

class PlantumlStep(): AbstractAtomicStep() {
    lateinit var parameters: Map<QName, XdmValue>

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        parameters = qnameMapBinding(Ns.parameters)
        val format = stringBinding(Ns.format) ?: "png"
        val baos = ByteArrayOutputStream()

        val fileFormat = when (format) {
            "png" -> FileFormat.PNG
            "svg" -> FileFormat.SVG
            "eps" -> FileFormat.EPS
            "eps-text" -> FileFormat.EPS_TEXT
            "atxt" -> FileFormat.ATXT
            "utxt" -> FileFormat.UTXT
            "xmi-standard" -> FileFormat.XMI_STANDARD
            "xmi-star" -> FileFormat.XMI_STAR
            "xmi-argo" -> FileFormat.XMI_ARGO
            "xmi-script" -> FileFormat.XMI_SCRIPT
            "scxml" -> FileFormat.SCXML
            "graphml" -> FileFormat.GRAPHML
            "pdf" -> FileFormat.PDF
            "html" -> FileFormat.HTML
            "html5" -> FileFormat.HTML5
            "vdx" -> FileFormat.VDX
            "latex" -> FileFormat.LATEX
            "latex-no-preamble" -> FileFormat.LATEX_NO_PREAMBLE
            "base64" -> FileFormat.BASE64
            "braille-png" -> FileFormat.BRAILLE_PNG
            "preproc" -> FileFormat.PREPROC
            "debug" -> FileFormat.DEBUG
            "raw" -> FileFormat.RAW
            else -> {
                stepConfig.warn { "Unexpected PlantUML format: ${format}; using png" }
                FileFormat.PNG
            }
        }

        try {
            val contentType = MediaType.parse(fileFormat.mimeType)

            val reader = SourceStringReader((source.value as XdmNode).underlyingValue.stringValue)
            val formatOption = FileFormatOption(fileFormat)
            val desc = reader.generateDiagramDescription(formatOption)
            reader.outputImage(baos, formatOption)

            if (desc == null) {
                throw stepConfig.exception(XProcError.xdStepFailed("PlantUML generator returned null"))
            }

            receiver.output("result", XProcDocument.ofBinary(baos.toByteArray(), stepConfig, contentType, DocumentProperties()))
        } catch (ex: Exception) {
            if (ex is XProcException) {
                throw ex
            }
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "???"), ex)
        }
    }

    override fun toString(): String = "cx:plantuml"
}
