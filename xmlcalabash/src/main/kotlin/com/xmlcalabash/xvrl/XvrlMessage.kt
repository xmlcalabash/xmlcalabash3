package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName

class XvrlMessage private constructor(stepConfiguration: StepConfiguration): XvrlElement(stepConfiguration) {
    private val _content = mutableListOf<XvrlElement>()
    val content: List<XvrlElement>
        get() = _content

    companion object {
        fun newInstance(stepConfig: StepConfiguration, text: String?, attr: Map<QName,String?> = emptyMap()): XvrlMessage {
            val message = XvrlMessage(stepConfig)
            message.commonAttributes(attr)
            text?.let { message._content.add(XvrlText(stepConfig, it)) }
            return message
        }
    }

    fun message(text: String): XvrlText {
        val xtext = XvrlText(stepConfig, text)
        _content.add(xtext)
        return xtext
    }

    fun message(name: QName, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
        val message = XvrlMessageElement.newInstance(stepConfig, name, null, attr)
        _content.add(message)
        return message
    }

    fun message(name: QName, text: String, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
        val message = XvrlMessageElement.newInstance(stepConfig, name, text, attr)
        _content.add(message)
        return message
    }

    fun message(name: QName, valueOf: XvrlValueOf, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
        val message = XvrlMessageElement.newInstance(stepConfig, name, valueOf, attr)
        _content.add(message)
        return message
    }

    fun message(name: QName, message: XvrlMessageElement, attr: Map<QName,String?> = emptyMap()): XvrlMessageElement {
        val message = XvrlMessageElement.newInstance(stepConfig, name, message, attr)
        _content.add(message)
        return message
    }

    fun clear() {
        _content.clear()
    }

    fun addContent(content: XvrlElement) {
        when (content) {
            is XvrlText, is XvrlMessageElement -> _content.add(content)
            else -> throw stepConfig.exception(XProcError.xiXvrlInvalidMessage("${content}"))
        }
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.message, stepConfig.typeUtils.attributeMap(attributes))
        for (item in content) {
            item.serialize(builder)
        }
        builder.addEndElement()
    }
}