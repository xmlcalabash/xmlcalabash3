package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsSaxon
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class XvrlDetection private constructor(stepConfig: StepConfiguration): XvrlElement(stepConfig) {
    companion object {
        val _severity = QName("severity")

        fun newInstance(stepConfig: StepConfiguration, severity: String, code: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlDetection {
            if (severity !in listOf("info", "warning", "error", "fatal-error", "unspecified")) {
                throw stepConfig.exception(XProcError.xiXvrlInvalidSeverity(severity))
            }

            val detection = XvrlDetection(stepConfig)
            detection.commonAttributes(attr)
            detection.setAttribute(_severity, severity)
            code?.let { detection.setAttribute(Ns.code, it) }
            return detection
        }
    }

    val severity: String
        get() = attributes[_severity]!!
    var code: String?
        get() = attributes[Ns.code]
        set(value) {
            if (value == null) {
                throw stepConfig.exception(XProcError.xiXvrlNullCode())
            }
            attributes[Ns.code] = value
        }

    var location: XvrlLocation? = null
    var provenance: XvrlProvenance? = null
    val title = mutableListOf<XvrlTitle>()
    val summary = mutableListOf<XvrlSummary>()
    val category = mutableListOf<XvrlCategory>()
    val lets = mutableListOf<XvrlLet>()
    val message = mutableListOf<XvrlMessage>()
    val supplemental = mutableListOf<XvrlSupplemental>()
    var context: XvrlContext? = null

    // ============================================================

    fun location(href: URI? = null, attr: Map<QName,String?> = emptyMap()): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href, attr)
        return location!!
    }

    fun location(href: URI?, line: Int, column: Int = 0, attr: Map<QName,String?> = emptyMap()): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href, line, column, attr)
        return location!!
    }

    fun location(href: URI?, offset: Int, attr: Map<QName,String?> = emptyMap()): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href, offset, attr)
        return location!!
    }

    fun location(location: Location, attr: Map<QName,String?> = emptyMap()): XvrlLocation {
        if (location.lineNumber > 0) {
            if (location.columnNumber > 0) {
                return XvrlLocation.newInstance(stepConfig, location.baseUri, location.lineNumber, location.columnNumber, attr)
            }
            return XvrlLocation.newInstance(stepConfig, location.baseUri, location.lineNumber, 0, attr)
        }
        return XvrlLocation.newInstance(stepConfig, location.baseUri, attr)
    }

    fun location(location: net.sf.saxon.s9api.Location, attr: Map<QName,String?> = emptyMap()): XvrlLocation {
        val localAttr = mutableMapOf<QName, String?>()
        localAttr[NsSaxon.publicIdentifier] = location.publicId
        localAttr.putAll(attr)
        if (location.lineNumber > 0) {
            if (location.columnNumber > 0) {
                return XvrlLocation.newInstance(stepConfig, URI(location.systemId), location.lineNumber, location.columnNumber, localAttr)
            }
            return XvrlLocation.newInstance(stepConfig, URI(location.systemId), location.lineNumber, 0, localAttr)
        }
        return XvrlLocation.newInstance(stepConfig, URI(location.systemId), localAttr)
    }

    // ============================================================

    fun provenance(): XvrlProvenance {
        if (provenance == null) {
            provenance = XvrlProvenance.newInstance(stepConfig)
        }
        return provenance!!
    }

    // ============================================================

    fun title(content: String, attr: Map<QName,String?> = emptyMap()): XvrlTitle {
        val xtitle = XvrlTitle.newInstance(stepConfig, content, attr)
        title.add(xtitle)
        return xtitle
    }

    fun title(content: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlTitle {
        val xtitle = XvrlTitle.newInstance(stepConfig, content, attr)
        title.add(xtitle)
        return xtitle
    }

    fun title(content: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlTitle {
        val xtitle = XvrlTitle.newInstance(stepConfig, content, attr)
        title.add(xtitle)
        return xtitle
    }

    // ============================================================

    fun summary(content: String, attr: Map<QName,String?> = emptyMap()): XvrlSummary {
        val sum = XvrlSummary.newInstance(stepConfig, content, attr)
        summary.add(sum)
        return sum
    }

    fun summary(content: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlSummary {
        val sum = XvrlSummary.newInstance(stepConfig, content, attr)
        summary.add(sum)
        return sum
    }

    fun summary(content: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlSummary {
        val sum = XvrlSummary.newInstance(stepConfig, content, attr)
        summary.add(sum)
        return sum
    }

    // ============================================================

    fun category(content: String?, vocabulary: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlCategory {
        val cat = XvrlCategory.newInstance(stepConfig, content, vocabulary, attr)
        category.add(cat)
        return cat
    }

    fun category(content: XdmNode, vocabulary: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlCategory {
        val cat = XvrlCategory.newInstance(stepConfig, content, vocabulary, attr)
        category.add(cat)
        return cat
    }

    fun category(content: List<XdmNode>, vocabulary: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlCategory {
        val cat = XvrlCategory.newInstance(stepConfig, content, vocabulary, attr)
        category.add(cat)
        return cat
    }

    // ============================================================

    fun let(name: QName, value: String, attr: Map<QName,String?> = emptyMap()): XvrlLet {
        val let = XvrlLet.newInstance(stepConfig, name, value)
        lets.add(let)
        return let
    }

    fun let(name: QName, value: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlLet {
        val let = XvrlLet.newInstance(stepConfig, name, value)
        lets.add(let)
        return let
    }

    fun let(name: QName, value: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlLet {
        val let = XvrlLet.newInstance(stepConfig, name, value)
        lets.add(let)
        return let
    }

    // ============================================================

    fun message(attr: Map<QName,String?> = emptyMap()): XvrlMessage {
        val msg = XvrlMessage.newInstance(stepConfig, null, attr)
        message.add(msg)
        return msg
    }

    fun message(text: String, attr: Map<QName,String?> = emptyMap()): XvrlMessage {
        val msg = XvrlMessage.newInstance(stepConfig, text, attr)
        message.add(msg)
        return msg
    }

    // ============================================================

    fun supplemental(content: String?, attr: Map<QName,String?> = emptyMap()): XvrlSupplemental {
        val sup = XvrlSupplemental.newInstance(stepConfig, content, attr)
        supplemental.add(sup)
        return sup
    }

    fun supplemental(content: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlSupplemental {
        val sup = XvrlSupplemental.newInstance(stepConfig, content, attr)
        supplemental.add(sup)
        return sup
    }

    fun supplemental(content: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlSupplemental {
        val sup = XvrlSupplemental.newInstance(stepConfig, content, attr)
        supplemental.add(sup)
        return sup
    }

    // ============================================================

    fun context(content: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlContext {
        context = XvrlContext.newInstance(stepConfig, content, attr)
        return context!!
    }

    fun context(content: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlContext {
        context = XvrlContext.newInstance(stepConfig, content, attr)
        return context!!
    }

    fun context(content: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlContext {
        context = XvrlContext.newInstance(stepConfig, content, attr)
        return context!!
    }

    // ============================================================

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.detection, stepConfig.typeUtils.attributeMap(attributes))
        location?.serialize(builder)
        provenance?.serialize(builder)
        for (item in title) {
            item.serialize(builder)
        }
        for (item in summary) {
            item.serialize(builder)
        }
        for (item in category) {
            item.serialize(builder)
        }
        for (item in lets) {
            item.serialize(builder)
        }
        for (item in message) {
            item.serialize(builder)
        }
        for (item in supplemental) {
            item.serialize(builder)
        }
        context?.serialize(builder)
        builder.addEndElement()
    }
}
