package com.xmlcalabash.steps.os

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.s9api.QName
import org.apache.logging.log4j.kotlin.logger

class OsInfo(): AbstractAtomicStep() {
    companion object {
        private val mapping = mapOf(
            "file.separator" to "file-separator",
            "path.separator" to "path-separator",
            "os.arch" to "os-architecture",
            "os.name" to "os-name",
            "os.version" to "os-version",
            "user.dir" to "cwd",
            "user.name" to "user-name",
            "user.home" to "user-home"
        )
    }

    var onlyStandardProperties = false

    override fun input(port: String, doc: XProcDocument) {
        // none
    }

    override fun extensionAttributes(attributes: Map<QName, String>) {
        super.extensionAttributes(attributes)
        val value = attributes[NsCx.onlyStandard]
        if (value != null) {
            if (value == "true" || value == "false") {
                onlyStandardProperties = value == "true"
            } else {
                logger.debug { "Ignoring unexpected value for cx:only-standard: ${value}"}
            }
        }
    }

    override fun run() {
        super.run()

        val attr = mutableMapOf<QName, String?>()
        for ((pname, pvalue) in System.getProperties()) {
            val name = pname.toString()
            val value = pvalue.toString()
            if (mapping.contains(name)) {
                attr[QName(mapping[name])] = value
            } else if (!onlyStandardProperties) {
                val qname = QName(NsCx.namespace, "cx:${name.replace(".", "-")}")
                attr[qname] = value
            }
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.result, stepConfig.attributeMap(attr))

        for ((name, value) in System.getenv()) {
            builder.addStartElement(NsC.environment, stepConfig.attributeMap(mapOf(
                Ns.name to name,
                Ns.value to value
            )))
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()

        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    override fun toString(): String = "p:os-info"
}