package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI
import java.time.ZonedDateTime

class XvrlReportMetadata private constructor(stepConfiguration: XProcStepConfiguration): XvrlElement(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, attr: Map<QName,String?> = emptyMap()): XvrlReportMetadata {
            val metadata = XvrlReportMetadata(stepConfig)
            metadata.commonAttributes(attr)
            return metadata
        }
    }
    var timestamp: XvrlTimestamp? = XvrlTimestamp.newInstance(stepConfig, ZonedDateTime.now())
    var validator: XvrlValidator? = null
    var creator: XvrlCreator? = null
    val document = mutableListOf<XvrlDocument>()
    val title = mutableListOf<XvrlTitle>()
    val summary = mutableListOf<XvrlSummary>()
    val category = mutableListOf<XvrlCategory>()
    val schema = mutableListOf<XvrlSchema>()
    val supplemental = mutableListOf<XvrlSupplemental>()

    fun timestamp(attr: Map<QName,String?> = emptyMap()): XvrlTimestamp {
        timestamp = XvrlTimestamp.newInstance(stepConfig, ZonedDateTime.now(), attr)
        return timestamp!!
    }

    fun timestamp(stamp: ZonedDateTime, attr: Map<QName,String?> = emptyMap()): XvrlTimestamp {
        timestamp = XvrlTimestamp.newInstance(stepConfig, stamp, attr)
        return timestamp!!
    }

    // ============================================================

    fun validator(name: String, attr: Map<QName,String?> = emptyMap()): XvrlValidator {
        validator = XvrlValidator.newInstance(stepConfig, name, null, attr)
        return validator!!
    }

    fun validator(name: String, version: String, attr: Map<QName,String?> = emptyMap()): XvrlValidator {
        validator = XvrlValidator.newInstance(stepConfig, name, version, attr)
        return validator!!
    }

    fun validator(name: String, version: String, text: String, attr: Map<QName,String?> = emptyMap()): XvrlValidator {
        validator = XvrlValidator.newInstance(stepConfig, name, version, text, attr)
        return validator!!
    }

    fun validator(name: String, version: String, node: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlValidator {
        validator = XvrlValidator.newInstance(stepConfig, name, version, node, attr)
        return validator!!
    }

    fun validator(name: String, version: String, nodes: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlValidator {
        validator = XvrlValidator.newInstance(stepConfig, name, version, nodes, attr)
        return validator!!
    }

    // ============================================================

    fun creator(name: String, attr: Map<QName,String?> = emptyMap()): XvrlCreator {
        creator = XvrlCreator.newInstance(stepConfig, name, null, attr)
        return creator!!
    }

    fun creator(name: String, version: String, attr: Map<QName,String?> = emptyMap()): XvrlCreator {
        creator = XvrlCreator.newInstance(stepConfig, name, version, attr)
        return creator!!
    }

    fun creator(name: String, version: String, text: String, attr: Map<QName,String?> = emptyMap()): XvrlCreator {
        creator = XvrlCreator.newInstance(stepConfig, name, version, text, attr)
        return creator!!
    }

    fun creator(name: String, version: String, node: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlCreator {
        creator = XvrlCreator.newInstance(stepConfig, name, version, node, attr)
        return creator!!
    }

    fun creator(name: String, version: String, nodes: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlCreator {
        creator = XvrlCreator.newInstance(stepConfig, name, version, nodes, attr)
        return creator!!
    }

    // ============================================================

    fun document(href: URI?, attr: Map<QName,String?> = emptyMap()): XvrlDocument {
        val doc = XvrlDocument.newInstance(stepConfig, href, attr)
        document.add(doc)
        return doc
    }

    fun document(href: URI?, content: String, attr: Map<QName,String?> = emptyMap()): XvrlDocument {
        val doc = XvrlDocument.newInstance(stepConfig, href, content, attr)
        document.add(doc)
        return doc
    }

    fun document(href: URI?, content: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlDocument {
        val doc = XvrlDocument.newInstance(stepConfig, href, content, attr)
        document.add(doc)
        return doc
    }

    fun document(href: URI?, content: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlDocument {
        val doc = XvrlDocument.newInstance(stepConfig, href, content, attr)
        document.add(doc)
        return doc
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

    fun schema(href: URI?, typens: NamespaceUri, version: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlSchema {
        val sch = XvrlSchema.newInstance(stepConfig, href, typens, version)
        schema.add(sch)
        return sch
    }

    fun schema(href: URI?, typens: NamespaceUri, version: String, content: String? = null, attr: Map<QName,String?> = emptyMap()): XvrlSchema {
        val sch = if (content == null) {
            XvrlSchema.newInstance(stepConfig, href, typens, version)
        } else {
            XvrlSchema.newInstance(stepConfig, href, typens, version, content)
        }
        schema.add(sch)
        return sch
    }

    fun schema(href: URI?, typens: NamespaceUri, version: String? = null, content: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlSchema {
        val sch = XvrlSchema.newInstance(stepConfig, href, typens, version, content)
        schema.add(sch)
        return sch
    }

    fun schema(href: URI?, typens: NamespaceUri, version: String? = null, content: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlSchema {
        val sch = XvrlSchema.newInstance(stepConfig, href, typens, version, content)
        schema.add(sch)
        return sch
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

    fun isEmpty(): Boolean {
        return timestamp == null && isEffectivelyEmpty()
    }

    fun isEffectivelyEmpty(): Boolean {
        return validator == null && creator == null
                && document.isEmpty() && title.isEmpty() && summary.isEmpty()
                && category.isEmpty() && schema.isEmpty() && supplemental.isEmpty()
    }

    // ============================================================

    override fun serialize(builder: SaxonTreeBuilder) {
        if (isEmpty()) {
            return
        }

        builder.addStartElement(NsXvrl.metadata, stepConfig.attributeMap(attributes))
        timestamp?.serialize(builder)
        validator?.serialize(builder)
        creator?.serialize(builder)
        for (item in document) {
            item.serialize(builder)
        }
        for (item in title) {
            item.serialize(builder)
        }
        for (item in summary) {
            item.serialize(builder)
        }
        for (item in category) {
            item.serialize(builder)
        }
        for (item in schema) {
            item.serialize(builder)
        }
        for (item in supplemental) {
            item.serialize(builder)
        }
        builder.addEndElement()
    }
}