package com.xmlcalabash.steps.validation

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.type.ValidationFailure
import java.util.*

class Errors(private val stepConfig: XProcStepConfiguration, val format: String) {
    companion object {
        val sourceUri = QName("source-uri")
        val sourceLine = QName("source-line")
        val sourceColumn = QName("source-column")
        val schemaPart = QName("schema-part")
        val constraintName = QName("constraint-name")
        val constraintClause = QName("constraint-clause")
        val schemaType = QName("schema-type")
    }
    private val builder = SaxonTreeBuilder(stepConfig)
    private val openStack = Stack<QName>()
    private val inLibrary = false
    private var nsmap = NamespaceMap.emptyMap()

    init {
        builder.startDocument(null)
        if (format == "xvrl") {
            nsmap = nsmap.put("xvrl", NsXvrl.namespace)
            nsmap = nsmap.put("err", NsFn.errorNamespace)
            builder.addStartElement(NsXvrl.report, EmptyAttributeMap.getInstance(), nsmap)
            openStack.push(NsXvrl.report)
        } else {
            nsmap = nsmap.put("c", NsC.namespace)
            nsmap = nsmap.put("err", NsFn.errorNamespace)
            builder.addStartElement(NsC.errors, EmptyAttributeMap.getInstance(), nsmap)
            openStack.push(NsC.errors)
        }
    }

    fun endErrors(): XdmNode {
        builder.addEndElement()
        openStack.pop()
        builder.endDocument()
        return builder.result
    }

    private fun end() {
        builder.addEndElement()
        openStack.pop()
    }

    fun xsdValidationError(msg: String, fail: ValidationFailure) {
        var nsmap = NamespaceMap.emptyMap()
        val amap = mutableMapOf<QName, String?>(
            Ns.message to msg,
            sourceUri to fail.systemId,
            Ns.path to fail.absolutePath?.toString(),
            constraintName to fail.constraintName,
            constraintClause to fail.constraintClauseNumber,
            schemaType to fail.schemaType?.toString()
        )

        if (fail.lineNumber > 0) {
            amap[sourceLine] = fail.lineNumber.toString()
            if (fail.columnNumber > 0) {
                amap[sourceColumn] = fail.columnNumber.toString()
            }
        }

        if (fail.errorCodeQName != null) {
            nsmap = nsmap.put("err", fail.errorCodeQName.namespaceUri)
            amap[Ns.code] = fail.errorCodeQName.displayName
        }

        if (fail.schemaPart >= 0) {
            amap[schemaPart] = fail.schemaPart.toString()
        }

        if (format == "xvrl") {
            builder.addStartElement(NsXvrl.detection, stepConfig.attributeMap(amap))
            builder.addEndElement()
        } else {
            builder.addStartElement(NsC.error, stepConfig.attributeMap(amap))
            builder.addEndElement()
        }
    }

    fun xsdValidationError(msg: String) {
        val amap = mapOf(Ns.message to msg)

        if (format == "xvrl") {
            builder.addStartElement(NsXvrl.detection, stepConfig.attributeMap(amap))
            builder.addEndElement()
        } else {
            builder.addStartElement(NsC.error, stepConfig.attributeMap(amap))
            builder.addEndElement()
        }
    }

    fun jsonValidationError(code: String, message: String) {
        val amap = mapOf(Ns.code to code, Ns.message to message)

        if (format == "xvrl") {
            builder.addStartElement(NsXvrl.detection, stepConfig.attributeMap(amap))
            builder.addEndElement()
        } else {
            builder.addStartElement(NsC.error, stepConfig.attributeMap(amap))
            builder.addEndElement()
        }
    }
}