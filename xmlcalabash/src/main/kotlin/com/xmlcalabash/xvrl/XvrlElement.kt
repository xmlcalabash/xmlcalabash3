package com.xmlcalabash.xvrl

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

abstract class XvrlElement(val stepConfig: XProcStepConfiguration) {
    companion object {
        val xpathDefaultNamespace = QName("xpath-default-namespace")

    }

    protected val attributes = mutableMapOf<QName, String>()

    protected fun setElementAttribute(name: QName, value: String) {
        attributes[name] = value
    }

    fun commonAttributes(attr: Map<QName, String>) {
        for ((name, value) in attr) {
            when (name) {
                NsXml.lang -> attributes[name] = value
                NsXml.id -> attributes[name] = value
                NsXml.base -> attributes[name] = value
                xpathDefaultNamespace -> attributes[name] = value
                else -> {
                    if (name.namespaceUri in listOf(NamespaceUri.NULL, NsXml.namespace, NsXvrl.namespace)) {
                        throw stepConfig.exception(XProcError.xiXvrlIllegalCommonAttribute(name))
                    }
                    attributes[name] = value
                }
            }
        }
    }

    open fun setAttribute(name: QName, value: String) {
        setElementAttribute(name, value)
    }

    open fun setAttributes(attr: Map<QName, String>) {
        for ((name, value) in attr) {
            setAttribute(name, value)
        }
    }

    fun getAttribute(name: QName): String? {
        return attributes[name]
    }

    open fun serialize(builder: SaxonTreeBuilder) {
        TODO("Not implemented on ${this}")
    }
}