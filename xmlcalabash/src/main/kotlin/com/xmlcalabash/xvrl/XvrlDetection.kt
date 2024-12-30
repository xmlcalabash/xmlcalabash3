package com.xmlcalabash.xvrl

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class XvrlDetection private constructor(stepConfig: XProcStepConfiguration): XvrlElement(stepConfig) {
    companion object {
        val _severity = QName("severity")

        fun newInstance(stepConfig: XProcStepConfiguration, severity: String, code: String? = null, attr: Map<QName,String> = emptyMap()): XvrlDetection {
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
    val code: String?
        get() = attributes[Ns.code]

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

    fun location(href: URI?): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href)
        return location!!
    }

    fun location(href: URI?, line: Int, column: Int = 0): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href, line, column)
        return location!!
    }

    fun location(href: URI?, offset: Int): XvrlLocation {
        location = XvrlLocation.newInstance(stepConfig, href, offset)
        return location!!
    }

    // ============================================================

    fun provenance(): XvrlProvenance {
        if (provenance == null) {
            provenance = XvrlProvenance.newInstance(stepConfig)
        }
        return provenance!!
    }

    // ============================================================

    fun title(content: String, attr: Map<QName, String> = emptyMap()): XvrlTitle {
        val xtitle = XvrlTitle.newInstance(stepConfig, content, attr)
        title.add(xtitle)
        return xtitle
    }

    fun title(content: XdmNode, attr: Map<QName, String> = emptyMap()): XvrlTitle {
        val xtitle = XvrlTitle.newInstance(stepConfig, content, attr)
        title.add(xtitle)
        return xtitle
    }

    fun title(content: List<XdmNode>, attr: Map<QName, String> = emptyMap()): XvrlTitle {
        val xtitle = XvrlTitle.newInstance(stepConfig, content, attr)
        title.add(xtitle)
        return xtitle
    }

    // ============================================================

    fun summary(content: String, attr: Map<QName, String> = emptyMap()): XvrlSummary {
        val sum = XvrlSummary.newInstance(stepConfig, content, attr)
        summary.add(sum)
        return sum
    }

    fun summary(content: XdmNode, attr: Map<QName, String> = emptyMap()): XvrlSummary {
        val sum = XvrlSummary.newInstance(stepConfig, content, attr)
        summary.add(sum)
        return sum
    }

    fun summary(content: List<XdmNode>, attr: Map<QName, String> = emptyMap()): XvrlSummary {
        val sum = XvrlSummary.newInstance(stepConfig, content, attr)
        summary.add(sum)
        return sum
    }

    // ============================================================

    fun category(content: String?, vocabulary: String? = null, attr: Map<QName, String> = emptyMap()): XvrlCategory {
        val cat = XvrlCategory.newInstance(stepConfig, content, vocabulary, attr)
        category.add(cat)
        return cat
    }

    fun category(content: XdmNode, vocabulary: String? = null, attr: Map<QName, String> = emptyMap()): XvrlCategory {
        val cat = XvrlCategory.newInstance(stepConfig, content, vocabulary, attr)
        category.add(cat)
        return cat
    }

    fun category(content: List<XdmNode>, vocabulary: String? = null, attr: Map<QName, String> = emptyMap()): XvrlCategory {
        val cat = XvrlCategory.newInstance(stepConfig, content, vocabulary, attr)
        category.add(cat)
        return cat
    }

    // ============================================================

    fun let(name: QName, value: String, attr: Map<QName, String> = emptyMap()): XvrlLet {
        val let = XvrlLet.newInstance(stepConfig, name, value)
        lets.add(let)
        return let
    }

    fun let(name: QName, value: XdmNode, attr: Map<QName, String> = emptyMap()): XvrlLet {
        val let = XvrlLet.newInstance(stepConfig, name, value)
        lets.add(let)
        return let
    }

    fun let(name: QName, value: List<XdmNode>, attr: Map<QName, String> = emptyMap()): XvrlLet {
        val let = XvrlLet.newInstance(stepConfig, name, value)
        lets.add(let)
        return let
    }

    // ============================================================

    fun message(content: String? = null, attr: Map<QName, String> = emptyMap()): XvrlMessage {
        val msg = XvrlMessage.newInstance(stepConfig, content, attr)
        message.add(msg)
        return msg
    }

    // ============================================================

    fun supplemental(content: String?, attr: Map<QName, String> = emptyMap()): XvrlSupplemental {
        val sup = XvrlSupplemental.newInstance(stepConfig, content, attr)
        supplemental.add(sup)
        return sup
    }

    fun supplemental(content: XdmNode, attr: Map<QName, String> = emptyMap()): XvrlSupplemental {
        val sup = XvrlSupplemental.newInstance(stepConfig, content, attr)
        supplemental.add(sup)
        return sup
    }

    fun supplemental(content: List<XdmNode>, attr: Map<QName, String> = emptyMap()): XvrlSupplemental {
        val sup = XvrlSupplemental.newInstance(stepConfig, content, attr)
        supplemental.add(sup)
        return sup
    }

    // ============================================================

    fun context(content: String? = null, attr: Map<QName, String> = emptyMap()): XvrlContext {
        context = XvrlContext.newInstance(stepConfig, content, attr)
        return context!!
    }

    fun context(content: XdmNode, attr: Map<QName, String> = emptyMap()): XvrlContext {
        context = XvrlContext.newInstance(stepConfig, content, attr)
        return context!!
    }

    fun context(content: List<XdmNode>, attr: Map<QName, String> = emptyMap()): XvrlContext {
        context = XvrlContext.newInstance(stepConfig, content, attr)
        return context!!
    }

    // ============================================================
    // ============================================================
    // ============================================================
    // ============================================================
    // ============================================================

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.detection, stepConfig.attributeMap(attributes))
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