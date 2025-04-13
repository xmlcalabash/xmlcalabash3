package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class XvrlSchema private constructor(stepConfiguration: StepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        private val _schematypens = QName("schematypens")

        fun newInstance(stepConfig: StepConfiguration, href: URI?, typens: NamespaceUri, version: String?, attr: Map<QName,String?> = emptyMap()): XvrlSchema {
            val schema = XvrlSchema(stepConfig)
            schema.setAttributes(attr)
            href?.let { schema.setAttribute(Ns.href, "${it}") }
            schema.setAttribute(_schematypens, "${typens}")
            version?.let { schema.setAttribute(Ns.version, it) }
            return schema
        }

        fun newInstance(stepConfig: StepConfiguration, href: URI?, typens: NamespaceUri, version: String?, content: String, attr: Map<QName,String?> = emptyMap()): XvrlSchema {
            val schema = XvrlSchema(stepConfig)
            schema.withText(content)
            schema.setAttributes(attr)
            href?.let { schema.setAttribute(Ns.href, "${it}") }
            schema.setAttribute(_schematypens, "${typens}")
            version?.let { schema.setAttribute(Ns.version, it) }
            return schema
        }

        fun newInstance(stepConfig: StepConfiguration, href: URI?, typens: NamespaceUri, version: String?, content: XdmNode, attr: Map<QName,String?> = emptyMap()): XvrlSchema {
            val schema = XvrlSchema(stepConfig)
            schema.withNode(content)
            schema.setAttributes(attr)
            href?.let { schema.setAttribute(Ns.href, "${it}") }
            schema.setAttribute(_schematypens, "${typens}")
            version?.let { schema.setAttribute(Ns.version, it) }
            return schema
        }


        fun newInstance(stepConfig: StepConfiguration, href: URI?, typens: NamespaceUri, version: String?, content: List<XdmNode>, attr: Map<QName,String?> = emptyMap()): XvrlSchema {
            val schema = XvrlSchema(stepConfig)
            schema.withNodes(content)
            schema.setAttributes(attr)
            href?.let { schema.setAttribute(Ns.href, "${it}") }
            schema.setAttribute(_schematypens, "${typens}")
            version?.let { schema.setAttribute(Ns.version, it) }
            return schema
        }
    }

    val href: URI?
        get() {
            val href = attributes[Ns.href]
            if (href == null) {
                return null
            }
            return URI.create(href)
        }

    val schematypens: NamespaceUri
        get() {
            return NamespaceUri.of(attributes[_schematypens]!!)
        }

    val version: String?
        get() = attributes[Ns.version]

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.schema, stepConfig.typeUtils.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }

}