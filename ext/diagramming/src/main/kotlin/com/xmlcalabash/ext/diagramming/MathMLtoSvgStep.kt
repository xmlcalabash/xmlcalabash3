package com.xmlcalabash.ext.diagramming

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import net.sourceforge.jeuclid.MathMLParserSupport
import net.sourceforge.jeuclid.context.LayoutContextImpl
import net.sourceforge.jeuclid.context.Parameter
import net.sourceforge.jeuclid.converter.Converter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class MathMLtoSvgStep(): AbstractAtomicStep() {
    lateinit var parameters: Map<QName, XdmValue>
    lateinit var params: LayoutContextImpl

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        parameters = qnameMapBinding(Ns.parameters)

        params = LayoutContextImpl(LayoutContextImpl.getDefaultLayoutContext())
        val conv = Converter.getInstance()

        setJParameter(Parameter.ANTIALIAS,                "antialias")
        setJParameter(Parameter.ANTIALIAS_MINSIZE,        "antialias-minsize")
        setJParameter(Parameter.DEBUG,                    "debug")
        setJParameter(Parameter.DISPLAY,                  "display")
        setJParameter(Parameter.FONTS_DOUBLESTRUCK,       "fonts-doublestruck")
        setJParameter(Parameter.FONTS_FRAKTUR,            "fonts-fraktur")
        setJParameter(Parameter.FONTS_MONOSPACED,         "fonts-monospaced")
        setJParameter(Parameter.FONTS_SANSSERIF,          "fonts-sansserif")
        setJParameter(Parameter.FONTS_SCRIPT,             "fonts-script")
        setJParameter(Parameter.FONTS_SERIF,              "fonts-serif")
        setJParameter(Parameter.MATHBACKGROUND,           "mathbackground")
        setJParameter(Parameter.MATHCOLOR,                "mathcolor")
        setJParameter(Parameter.MATHSIZE,                 "mathsize")
        setJParameter(Parameter.MFRAC_KEEP_SCRIPTLEVEL,   "mfrac-keep-scriptlevel")
        setJParameter(Parameter.SCRIPTLEVEL,              "scriptlevel")
        setJParameter(Parameter.SCRIPTMINSIZE,            "scriptminsize")
        setJParameter(Parameter.SCRIPTSIZEMULTIPLIER,     "scriptsizemultiplier")

        // Hack. Back in the version 1.0 days, I failed to get the DOM wrapper around NodeInfo to work...
        try {
            val baos = ByteArrayOutputStream()
            val writer = DocumentWriter(source, baos)
            writer.write()
            val mathML = baos.toString(StandardCharsets.UTF_8)

            val svgbaos = ByteArrayOutputStream()
            val jdoc = MathMLParserSupport.parseString(mathML)
            val dim = conv.convert(jdoc, svgbaos, Converter.TYPE_SVG, params)

            val props = DocumentProperties()
            props.set(QName(NamespaceUri.NULL, "width"), "${dim.width}")
            props.set(QName(NamespaceUri.NULL, "height"), "${dim.height}")

            val loader = DocumentLoader(stepConfig, null, props)
            val bais = ByteArrayInputStream(svgbaos.toByteArray())
            val svg = loader.load(bais, MediaType.SVG)
            receiver.output("result", svg)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "???"), ex)
        }
    }

    private fun setJParameter(param: Parameter, key: String) {
        val name = QName(NamespaceUri.NULL, key)
        if (name in parameters) {
            params.setParameter(param, parameters[name]!!)
        }
    }

    override fun toString(): String = "cx:plantuml"
}