package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.stathissideris.ascii2image.core.ConversionOptions
import org.stathissideris.ascii2image.core.ProcessingOptions
import org.stathissideris.ascii2image.graphics.BitmapRenderer
import org.stathissideris.ascii2image.graphics.Diagram
import org.stathissideris.ascii2image.text.TextGrid
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageOutputStream

class DitaaStep(): AbstractAtomicStep() {
    companion object {
        private val _shadows = QName(NamespaceUri.NULL, "shadows")
        private val _antialias = QName(NamespaceUri.NULL, "antialias")
        private val _corners = QName(NamespaceUri.NULL, "corners")
        private val _separation = QName(NamespaceUri.NULL, "separation")
        private val _scale = QName(NamespaceUri.NULL, "scale")
    }

    lateinit var parameters: Map<QName, XdmValue>

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        parameters = qnameMapBinding(Ns.parameters)
        val contentType = mediaTypeBinding(Ns.contentType, MediaType.PNG)

        try {
            val cOptions = ConversionOptions()
            val pOptions = ProcessingOptions()
            pOptions.characterEncoding = "UTF-8"
            cOptions.renderingOptions.setDropShadows(boolOption(_shadows))
            pOptions.setPerformSeparationOfCommonEdges(boolOption(_separation))
            cOptions.renderingOptions.setAntialias(boolOption(_antialias))

            if (_corners in parameters) {
                val value = parameters[_corners]!!.underlyingValue.stringValue
                if (value == "rounded" || value == "square") {
                    pOptions.setAllCornersAreRound(value == "rounded")
                } else {
                    stepConfig.warn { "Ignoring unexpected value ${value} for corners" }
                }
            }

            if (_scale in parameters) {
                // FIXME: this could actually be passed as a non-string value
                // Should string values even be allowed?
                val value = parameters[_scale]!!.underlyingValue.stringValue
                try {
                    cOptions.renderingOptions.scale = value.toFloat()
                } catch (_: NumberFormatException) {
                    stepConfig.warn { "Ignoring invalid value ${value} for scale" }
                }
            }

            val grid = TextGrid()
            if (pOptions.customShapes != null) {
                grid.addToMarkupTags(pOptions.customShapes.keys)
            }

            val lines = ArrayList<StringBuffer>()
            for (line in (source.value as XdmNode).underlyingValue.stringValue.trim().split("\n")) {
                lines.add(StringBuffer(line))
            }

            grid.initialiseWithLines(lines, pOptions)

            val diagram = Diagram(grid, cOptions, pOptions)
            val image = BitmapRenderer().renderToImage(diagram, cOptions.renderingOptions)
            val baos = ByteArrayOutputStream()

            val writerIter = ImageIO.getImageWritersByMIMEType("${contentType}")
            if (writerIter == null || !writerIter.hasNext()) {
                throw stepConfig.exception(XProcError.xdStepFailed("No image writer found for ${contentType}"))
            }
            val writer = writerIter.next()!!
            val stream = MemoryCacheImageOutputStream(baos)
            writer.output = stream
            writer.write(image)
            stream.close()

            receiver.output("result", XProcDocument.ofBinary(baos.toByteArray(), stepConfig, MediaType.PNG, DocumentProperties()))
        } catch (ex: Exception) {
            if (ex is XProcException) {
                throw ex
            }
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "???"), ex)
        }
    }

    private fun boolOption(name: QName): Boolean {
        if (name in parameters) {
            val value = parameters[name]!!.underlyingValue.stringValue
            if (value == "true" || value == "false") {
                return value == "true"
            }
            stepConfig.warn { "Ignoring non-boolean value for ${name}" }
        }
        return true
    }

    override fun toString(): String = "cx:ditaa"
}
