package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName

class XvrlMessageElement private constructor(stepConfiguration: StepConfiguration): XvrlElement(stepConfiguration) {
    private val _content = mutableListOf<XvrlElement>()
    val content: List<XvrlElement>
        get() = _content

    companion object {
        fun newInstance(stepConfig: StepConfiguration, name: QName, text: String?, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
            if (name.namespaceUri in listOf(NsXvrl.namespace)) {
                throw stepConfig.exception(XProcError.xiXvrlIllegalMessageName(name))
            }
            val message = XvrlMessageElement(stepConfig)
            message.setAttributes(attr)
            message.setAttribute(Ns.name, "${name}")
            text?.let { message._content.add(XvrlText(stepConfig, it)) }
            message.setAttributes(attr)
            return message
        }

        fun newInstance(stepConfig: StepConfiguration, name: QName, valueOf: XvrlValueOf, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
            if (name.namespaceUri in listOf(NsXvrl.namespace)) {
                throw stepConfig.exception(XProcError.xiXvrlIllegalMessageName(name))
            }
            val message = XvrlMessageElement(stepConfig)
            message.setAttributes(attr)
            message.setAttribute(Ns.name, "${name}")
            message._content.add(valueOf)
            return message
        }

        fun newInstance(stepConfig: StepConfiguration, name: QName, message: XvrlMessageElement, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
            if (name.namespaceUri in listOf(NsXvrl.namespace)) {
                throw stepConfig.exception(XProcError.xiXvrlIllegalMessageName(name))
            }
            val message = XvrlMessageElement(stepConfig)
            message.setAttributes(attr)
            message.setAttribute(Ns.name, "${name}")
            message._content.add(message)
            return message
        }
    }

    val name: QName
        get() {
            return stepConfig.typeUtils.parseQName(attributes[Ns.name]!!)
        }

    fun clear() {
        _content.clear()
    }

    fun message(name: QName, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
        val message = XvrlMessageElement.newInstance(stepConfig, name, null, attr)
        return message
    }

    /*
            fun newInstance(stepConfig: StepConfiguration, name: QName, text: String?, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
            if (name.namespaceUri in listOf(NsXvrl.namespace)) {
                throw stepConfig.exception(XProcError.xiXvrlIllegalMessageName(name))
            }
            val message = XvrlMessageElement(stepConfig)
            message.setAttributes(attr)
            message.setAttribute(Ns.name, "${name}")
            text?.let { message._content.add(XvrlText(stepConfig, it)) }
            message.setAttributes(attr)
            return message
        }

        fun newInstance(stepConfig: StepConfiguration, name: QName, valueOf: XvrlValueOf, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
            if (name.namespaceUri in listOf(NsXvrl.namespace)) {
                throw stepConfig.exception(XProcError.xiXvrlIllegalMessageName(name))
            }
            val message = XvrlMessageElement(stepConfig)
            message.setAttributes(attr)
            message.setAttribute(Ns.name, "${name}")
            message._content.add(valueOf)
            return message
        }

        fun newInstance(stepConfig: StepConfiguration, name: QName, message: XvrlMessageElement, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
            if (name.namespaceUri in listOf(NsXvrl.namespace)) {
                throw stepConfig.exception(XProcError.xiXvrlIllegalMessageName(name))
            }
            val message = XvrlMessageElement(stepConfig)
            message.setAttributes(attr)
            message.setAttribute(Ns.name, "${name}")
            message._content.add(message)
            return message
        }

     */

    fun addContent(content: XvrlElement) {
        when (content) {
            is XvrlText, is XvrlMessageElement, is XvrlValueOf -> _content.add(content)
            else -> throw stepConfig.exception(XProcError.xiXvrlInvalidMessage("${content}"))
        }
    }

    override fun setAttribute(name: QName, value: String) {
        if (name.namespaceUri == NsXvrl.namespace || name.namespaceUri == NsXml.namespace) {
            throw stepConfig.exception(XProcError.xiXvrlIllegalMessageAttribute(name))
        }
        setElementAttribute(name, value)
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(name, stepConfig.typeUtils.attributeMap(attributes))
        for (item in content) {
            item.serialize(builder)
        }
        builder.addEndElement()
    }
}