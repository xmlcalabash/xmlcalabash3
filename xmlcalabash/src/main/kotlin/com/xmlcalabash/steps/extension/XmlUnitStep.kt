package com.xmlcalabash.steps.extension

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.*
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.MarkdownConfigurer
import com.xmlcalabash.xvrl.XvrlLocation
import com.xmlcalabash.xvrl.XvrlReport
import com.xmlcalabash.xvrl.XvrlReportMetadata
import net.sf.saxon.dom.DocumentOverNodeInfo
import net.sf.saxon.s9api.*
import net.sf.saxon.value.QNameValue
import org.w3c.dom.Document
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.Comparison
import org.xmlunit.diff.ComparisonListener
import org.xmlunit.diff.ComparisonResult
import org.xmlunit.diff.DefaultNodeMatcher
import org.xmlunit.diff.Difference
import org.xmlunit.diff.ElementSelector
import org.xmlunit.diff.ElementSelectors
import org.xmlunit.diff.NodeMatcher

class XmlUnitStep(): AbstractAtomicStep() {
    companion object {
        private val _ignoreComments = QName("ignore-comments")
        private val _ignoreWhitespace = QName("ignore-whitespace")
        private val _normalizeWhitespace = QName("normalize-whitespace")
        private val _checkFor = QName("check-for")
        private val _elementSelector = QName("element-selector")
        private val _attributeList = QName("attribute-list")
        private val _nodeMatcherClass = QName("node-matcher-class")
        private val _elementSelectorClass = QName("element-selector-class")
    }
    lateinit var source: XProcDocument
    lateinit var alternate: XProcDocument

    override fun run() {
        super.run()

        source = queues["source"]!!.first()
        alternate = queues["alternate"]!!.first()

        val reportFormat = stringBinding(Ns.reportFormat) ?: "xvrl"
        val ignoreComments = booleanBinding(_ignoreComments) == true
        val ignoreWhitespace = booleanBinding(_ignoreWhitespace) == true
        val normalizeWhitespace = booleanBinding(_normalizeWhitespace) == true
        val failIfNotEqual = booleanBinding(Ns.failIfNotEqual) == true
        val checkFor = stringBinding(_checkFor)
        val elementSelector = stringBinding(_elementSelector)
        val nodeMatcherClass = stringBinding(_nodeMatcherClass)
        val elementSelectorClass = stringBinding(_elementSelectorClass)

        if (reportFormat != "xvrl") {
            throw stepConfig.exception(XProcError.xcUnsupportedReportFormat(reportFormat))
        }

        val attributeList = mutableListOf<javax.xml.namespace.QName>()
        if (_attributeList in options) {
            val value = options[_attributeList]!!.value
            for (item in value.iterator()) {
                if (item.underlyingValue is QNameValue) {
                    val qnameValue = item.underlyingValue as QNameValue
                    attributeList.add(javax.xml.namespace.QName(qnameValue.namespaceURI.toString(), qnameValue.localName, qnameValue.prefix))
                } else {
                    throw stepConfig.exception(XProcError.xcxXmlUnitBadAttributeList(item.toString()))
                }
            }
        }
        if (attributeList.isNotEmpty() && elementSelector != "by-name-and-attributes") {
            throw stepConfig.exception(XProcError.xcxXmlUnitAttributesSelector())
        }

        val elementSelectorImpl = if (elementSelectorClass != null) {
            if (elementSelector != null) {
                throw stepConfig.exception(XProcError.xcxXmlUnitElementSelectorAndClass())
            } else {
                val klass = Class.forName(elementSelectorClass)
                try {
                    klass.getConstructor().newInstance() as ElementSelector
                } catch (ex: Exception) {
                    throw stepConfig.exception(XProcError.xcxXmlUnitNotAnElementSelector(elementSelectorClass), ex)
                }
            }
        } else {
            when (elementSelector) {
                null, "by-name" -> ElementSelectors.byName
                "by-name-and-text" -> ElementSelectors.byNameAndText
                "by-name-and-all-attributes" -> ElementSelectors.byNameAndAllAttributes
                "by-name-and-attributes" -> {
                    val namelist = attributeList.toTypedArray()
                    ElementSelectors.byNameAndAttributes(*namelist)
                }
                else -> throw stepConfig.exception(XProcError.xcxXmlUnitUnexpectedElementSelector(elementSelector))
            }
        }

        val nodeMatcherImpl = if (nodeMatcherClass != null) {
            if (elementSelector != null) {
                throw stepConfig.exception(XProcError.xcxXmlUnitElementSelectorAndNodeMatcher())
            }
            val klass = Class.forName(nodeMatcherClass)
            try {
                klass.getConstructor().newInstance() as NodeMatcher
            } catch (ex: Exception) {
                throw stepConfig.exception(XProcError.xcxXmlUnitNotANodeMatcher(nodeMatcherClass), ex)
            }
        } else {
            DefaultNodeMatcher(elementSelectorImpl)
        }

        val sourceDoc = DocumentOverNodeInfo.wrap((source.value as XdmNode).underlyingNode) as Document
        val altDoc = DocumentOverNodeInfo.wrap((alternate.value as XdmNode).underlyingNode) as Document

        val control = Input.fromDocument(sourceDoc).build()
        val test = Input.fromDocument(altDoc).build()

        var diffBuilder = DiffBuilder.compare(control).withTest(test)
        if (checkFor == "similarity") {
            diffBuilder = diffBuilder.checkForSimilar()
        } else {
            diffBuilder = diffBuilder.checkForIdentical()
        }
        diffBuilder = diffBuilder.withNodeMatcher(nodeMatcherImpl)

        if (ignoreComments) {
            diffBuilder = diffBuilder.ignoreComments()
        }

        if (ignoreWhitespace) {
            diffBuilder = diffBuilder.ignoreWhitespace()
        }

        if (normalizeWhitespace) {
            diffBuilder = diffBuilder.normalizeWhitespace()
        }

        val diff = diffBuilder.build()

        val diffmeta = mutableMapOf<QName, String>()
        diffmeta[cx(_ignoreComments)] = "${ignoreComments}"
        diffmeta[cx(_ignoreWhitespace)] = "${ignoreWhitespace}"
        diffmeta[cx(_normalizeWhitespace)] = "${normalizeWhitespace}"
        diffmeta[cx(_checkFor)] = "${checkFor}"
        diffmeta[cx(_elementSelector)] = elementSelectorClass ?: elementSelector ?: "by-name"
        nodeMatcherClass?.let { diffmeta[cx(_nodeMatcherClass)] = it }

        if (attributeList.isNotEmpty()) {
            val sb = StringBuilder()
            for (attr in attributeList) {
                sb.append(attr.toString()).append(" ")
            }
            diffmeta[cx(_attributeList)] = sb.toString().trim()
        }

        val report = XvrlReport.newInstance(stepConfig, diffmeta)
        report.metadata.validator("xmlunit", XmlCalabashBuildConfig.DEPENDENCIES["xmlunit"] ?: "unknown")

        for (comp in diff.differences) {
            details(source, report, comp, comp.comparison.controlDetails)
            details(alternate, report, comp, comp.comparison.testDetails)
        }

        val reportDoc = XProcDocument.ofXml(report.asXml(stepConfig.baseUri), stepConfig, MediaType.XML)

        if (failIfNotEqual && diff.hasDifferences()) {
            throw stepConfig.exception(XProcError.xcxXmlUnitComparisonFailed(reportDoc))
        }

        receiver.output("result", source)
        receiver.output("report", reportDoc)
    }

    private fun details(doc: XProcDocument, report: XvrlReport, diff: Difference, details: Comparison.Detail) {
        val severity = when (diff.result) {
            ComparisonResult.EQUAL -> "info"
            ComparisonResult.SIMILAR -> "warning"
            else -> "error"
        }

        val loc = XvrlLocation.newInstance(stepConfig, doc.baseURI)
        if (details.xPath != null) {
            loc.xpath = details.xPath
        }

        val message = diff.toString()
        val pos = message.indexOf(" - comparing")
        val detection = if (pos >= 0) {
            report.detection(severity, null, message.substring(0, pos))
        } else {
            report.detection(severity, null, message)
        }

        detection.location = loc
    }

    private fun cx(name: QName): QName {
        return QName(NsCx.namespace, "cx:${name.localName}")
    }

    override fun toString(): String = "cx:xmlunit"
}