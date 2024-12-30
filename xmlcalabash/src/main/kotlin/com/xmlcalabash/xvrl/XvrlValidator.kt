package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class XvrlValidator private constructor(stepConfiguration: XProcStepConfiguration): XvrlContainer(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, name: String, version: String?, attr: Map<QName,String> = emptyMap()): XvrlValidator {
            val validator = XvrlValidator(stepConfig)
            validator.setAttributes(attr)
            validator.setAttribute(Ns.name, name)
            version?.let { validator.setAttribute(Ns.version, it) }
            return validator
        }

        fun newInstance(stepConfig: XProcStepConfiguration, name: String, version: String?, node: XdmNode, attr: Map<QName,String> = emptyMap()): XvrlValidator {
            val validator = XvrlValidator(stepConfig)
            validator.withNode(node)
            validator.setAttributes(attr)
            validator.setAttribute(Ns.name, name)
            version?.let { validator.setAttribute(Ns.version, it) }
            return validator
        }

        fun newInstance(stepConfig: XProcStepConfiguration, name: String, version: String?, text: String, attr: Map<QName,String> = emptyMap()): XvrlValidator {
            val validator = XvrlValidator(stepConfig)
            validator.withText(text)
            validator.setAttributes(attr)
            validator.setAttribute(Ns.name, name)
            version?.let { validator.setAttribute(Ns.version, it) }
            return validator
        }

        fun newInstance(stepConfig: XProcStepConfiguration, name: String, version: String?, nodes: List<XdmNode>, attr: Map<QName,String> = emptyMap()): XvrlValidator {
            val validator = XvrlValidator(stepConfig)
            validator.withNodes(nodes)
            validator.setAttributes(attr)
            validator.setAttribute(Ns.name, name)
            version?.let { validator.setAttribute(Ns.version, it) }
            return validator
        }
    }

    val name: String
        get() = attributes[Ns.name]!!

    val version: String?
        get() = attributes[Ns.version]

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.validator, stepConfig.attributeMap(attributes))
        serializeContent(builder)
        builder.addEndElement()
    }

}