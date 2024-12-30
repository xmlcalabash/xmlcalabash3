package com.xmlcalabash.xvrl

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName

class XvrlDigest internal constructor(stepConfig: XProcStepConfiguration): XvrlElement(stepConfig) {
    companion object {
        val _fatalErrorCount = QName("fatal-error-count")
        val _errorCount = QName("error-count")
        val _warningCount = QName("warning-count")
        val _infoCount = QName("info-count")
        val _unspecifiedCount = QName("unspecified-count")
        val _fatalErrorCodes = QName("fatal-error-codes")
        val _errorCodes = QName("error-codes")
        val _warningCodes = QName("warning-codes")
        val _infoCodes = QName("info-codes")
        val _unspecifiedCodes = QName("unspecified-codes")

        fun newInstance(stepConfig: XProcStepConfiguration, attr: Map<QName, String> = emptyMap()): XvrlDigest {
            val digest = XvrlDigest(stepConfig)
            digest.setAttributes(attr)
            return digest
        }
    }

    private var _valid: String = "undetermined"
    var valid: String
        get() = _valid
        set(value) {
            if (value !in listOf("true", "false", "partial", "undetermined")) {
                throw stepConfig.exception(XProcError.xiXvrlInvalidValid(value))
            }
            _valid = value
        }

    private var _worst: String = "unspecified"
    var worst: String
        get() = _worst
        set(value) {
            if (value !in listOf("fatal-error", "error", "warning", "info", "nothing")) {
                throw stepConfig.exception(XProcError.xiXvrlInvalidWorst(value))
            }
            when (_worst) {
                "unspecified" -> _worst = value
                "fatal-error" -> Unit
                "error" -> {
                    if (value == "fatal-error") {
                        _worst = value
                    }
                }
                "warning" -> {
                    if (value == "fatal-error" || value == "error") {
                        _worst = value
                    }
                }
                "info" -> {
                    if (value == "fatal-error" || value == "error" || value == "warning") {
                        _worst = value
                    }
                }
                "nothing" -> _worst = value
            }
        }

    var fatalErrorCount = 0
    var errorCount = 0
    var warningCount = 0
    var infoCount = 0
    var unspecifiedCount = 0
    var fatalErrorCodes = mutableSetOf<String>()
    var errorCodes = mutableSetOf<String>()
    var warningCodes = mutableSetOf<String>()
    var infoCodes = mutableSetOf<String>()
    var unspecifiedCodes = mutableSetOf<String>()

    override fun serialize(builder: SaxonTreeBuilder) {
        val attr = mutableMapOf<QName, String>()
        attr.putAll(attributes)
        attr[QName("valid")] = valid
        attr[_fatalErrorCount] = "${fatalErrorCount}"
        attr[_errorCount] = "${errorCount}"
        attr[_warningCount] = "${warningCount}"
        attr[_infoCount] = "${infoCount}"
        attr[_unspecifiedCount] = "${unspecifiedCount}"
        attr[_fatalErrorCodes] = fatalErrorCodes.joinToString(separator=" ")
        attr[_errorCodes] = errorCodes.joinToString(separator=" ")
        attr[_warningCodes] = warningCodes.joinToString(separator=" ")
        attr[_infoCodes] = infoCodes.joinToString(separator=" ")
        attr[_unspecifiedCodes] = unspecifiedCodes.joinToString(separator=" ")
        attr[QName("worst")] = worst
        builder.addStartElement(NsXvrl.digest, stepConfig.attributeMap(attr))
        builder.addEndElement()
    }
}