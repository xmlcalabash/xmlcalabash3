package com.xmlcalabash.ext.trang

import com.thaiopensource.relaxng.input.InputFormat
import com.thaiopensource.relaxng.input.dtd.DtdInputFormat
import com.thaiopensource.relaxng.input.parse.compact.CompactParseInputFormat
import com.thaiopensource.relaxng.input.parse.sax.SAXParseInputFormat
import com.thaiopensource.relaxng.output.OutputFormat
import com.thaiopensource.relaxng.output.dtd.DtdOutputFormat
import com.thaiopensource.relaxng.output.rnc.RncOutputFormat
import com.thaiopensource.relaxng.output.rng.RngOutputFormat
import com.thaiopensource.relaxng.output.xsd.XsdOutputFormat
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmMap
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException
import java.net.URI

abstract class AbstractTrangStep(): AbstractAtomicStep() {
    companion object {
        private val sourceParameters = QName("source-parameters")
        private val resultParameters = QName("result-parameters")

        val magicBaseUri = URI.create("http://xmlcalabash.com/magic/trang")

        val _sourceFormat = QName("source-format")
        val _namespaces = QName("namespaces")
        val _resultFormat = QName("result-format")
        val _outputBaseUri = QName("output-base-uri")

        val _annotationPrefix = QName("annotation-prefix")
        val _anyAttributeProcessContents = QName("any-attribute-process-contents")
        val _anyName = QName("any-name")
        val _anyProcessContents = QName("any-process-contents")
        val _attlistDefine = QName("attlist-define")
        val _colonReplacement = QName("colon-replacement")
        val _disableAbstractElements = QName("disable-abstract-elements")
        val _elementDefine = QName("element-define")
        val _encoding = QName("encoding")
        val _generateStart = QName("generate-start")
        val _indent = QName("indent")
        val _inlineAttlist = QName("inline-attlist")
        val _lineLength = QName("lineLength")
        val _strictAny = QName("strict-any")
    }

    var sourceFormat: String? = null
    var resultFormat: String? = null

    val inputOptions = mutableListOf<String>()
    val outputOptions = mutableListOf<String>()
    var outputEncoding = "UTF-8"
    var lineLength = 72
    var indent = 2

    protected fun parseNamespaces() {
        val namespaceMap = options[_namespaces]!!.value
        if (namespaceMap != XdmEmptySequence.getInstance()) {
            if (sourceFormat != "dtd") {
                stepConfig.warn { "Namespaces are only supported for DTD inputs" }
            }

            val map = namespaceMap as XdmMap
            for (key in map.keySet()) {
                val prefix = key.underlyingValue.stringValue
                val uri = map.get(key).underlyingValue.stringValue
                if (prefix == "") {
                    inputOptions.add("xmlns=${uri}")
                } else {
                    inputOptions.add("xmlns:${prefix}=${uri}")
                }
            }
        }
    }

    protected fun parseParameters() {
        when (sourceFormat!!) {
            "dtd" -> parseDtdSourceParameters()
            "rng", "rnc" -> parseRnxSourceParameters()
            else -> Unit
        }
        parseResultParameters()
    }

    private fun parseDtdSourceParameters() {
        val parameters = qnameMapBinding(sourceParameters)
        for ((key, value) in parameters) {
            val svalue = value.underlyingValue.stringValue
            when (key) {
                _generateStart, _inlineAttlist -> {
                    if (booleanOption(key.localName, svalue)) {
                        outputOptions.add("${key.localName}")
                    } else {
                        outputOptions.add("no-${key.localName}")
                    }
                }
                _strictAny -> {
                    if (booleanOption(key.localName, svalue)) {
                        inputOptions.add("${key.localName}")
                    }
                }
                _anyName, _annotationPrefix, _attlistDefine, _colonReplacement, _elementDefine -> inputOptions.add("${key.localName}=${svalue}")
                else -> stepConfig.warn { "Unrecognized Trang source parameter: ${key}" }
            }
        }
    }

    private fun parseRnxSourceParameters() {
        val parameters = qnameMapBinding(sourceParameters)
        for ((key, value) in parameters) {
            val svalue = value.underlyingValue.stringValue
            when (key) {
                _encoding -> { inputOptions.add("encoding=${svalue}") }
                else -> stepConfig.warn { "Unrecognized Trang source parameter: ${key}" }
            }
        }
    }

    protected fun parseResultParameters() {
        val parameters = qnameMapBinding(resultParameters)
        for ((key, value) in parameters) {
            val svalue = value.underlyingValue.stringValue
            when (key) {
                _anyAttributeProcessContents, _anyProcessContents -> {
                    if (resultFormat == "xsd") {
                        if (svalue in listOf("strict", "lax", "skip")) {
                            outputOptions.add("${key.localName}=${svalue}")
                        } else {
                            throw stepConfig.exception(XProcError.xcxTrangInvalidOptionValue(key.localName, svalue))
                        }
                    } else {
                        stepConfig.warn { "The ${key.localName} parameter only applies to XML Schema output" }
                    }
                }
                _disableAbstractElements -> {
                    if (resultFormat == "xsd") {
                        if (booleanOption(key.localName, svalue)) {
                            outputOptions.add("${key.localName}")
                        }
                    } else {
                        stepConfig.warn { "The ${key.localName} parameter only applies to XML Schema output" }
                    }
                }
                _encoding -> outputEncoding = svalue
                _indent -> indent = svalue.toInt()
                _lineLength -> lineLength = svalue.toInt()
                else -> stepConfig.warn { "Unrecognized Trang result parameter: ${key}" }
            }
        }
    }

    private fun booleanOption(name: String, value: String): Boolean {
        if (value in listOf("true", "false")) {
            return value == "true"
        } else {
            throw stepConfig.exception(XProcError.xcxTrangInvalidOptionValue(name, value))
        }
    }

    protected fun trangInputFormat(): InputFormat {
        return when (sourceFormat) {
            "rnc" -> CompactParseInputFormat()
            "rng" -> SAXParseInputFormat()
            "dtd" -> DtdInputFormat()
            else -> throw stepConfig.exception(XProcError.xcxTrangUnsupportedInputFormat(sourceFormat!!))
        }
    }

    protected fun trangOutputFormat(): OutputFormat {
        return when (resultFormat) {
            "rnc" -> RncOutputFormat()
            "rng" -> RngOutputFormat()
            "xsd" -> XsdOutputFormat()
            "dtd" -> DtdOutputFormat()
            else -> throw stepConfig.exception(XProcError.xcxTrangUnsupportedOutputFormat(resultFormat!!))
        }
    }

    inner class TrangErrorHandler: ErrorHandler {
        override fun warning(exception: SAXParseException?) {
            exception?.message?.let { stepConfig.warn { it }}
        }

        override fun error(exception: SAXParseException?) {
            if (exception == null) {
                throw stepConfig.exception(XProcError.xdStepFailed("unknown exception"))
            }
            throw stepConfig.exception(XProcError.xdStepFailed(exception.message ?: "unknown exception"), exception)
        }

        override fun fatalError(exception: SAXParseException?) {
            if (exception == null) {
                throw stepConfig.exception(XProcError.xdStepFailed("unknown exception"))
            }
            throw stepConfig.exception(XProcError.xdStepFailed(exception.message ?: "unknown exception"), exception)
        }
    }
}