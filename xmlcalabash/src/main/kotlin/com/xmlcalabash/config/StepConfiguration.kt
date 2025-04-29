package com.xmlcalabash.config

import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.DocumentContext
import com.xmlcalabash.util.TypeUtils
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.ValidationMode
import net.sf.saxon.s9api.XPathCompiler

open class StepConfiguration(val saxonConfig: SaxonConfiguration,
                             val context: DocumentContext,
                             val environment: XProcEnvironment) : XProcEnvironment by environment, DocumentContext by context {
    protected val _inscopeStepTypes = mutableMapOf<QName, DeclareStepInstruction>()
    val inscopeStepTypes: Map<QName, DeclareStepInstruction> = _inscopeStepTypes

    var validationMode = ValidationMode.DEFAULT

    var _qnameMapType: SequenceType? = null
    val qnameMapType: SequenceType
        get() {
            if (_qnameMapType == null) {
                _qnameMapType = typeUtils.parseXsSequenceType("map(xs:QName,item()*)")
            }
            return _qnameMapType!!
        }

    private var _stepName: String? = null
    var stepName: String
        get() = _stepName!!
        set(value) {
            _stepName = value
        }

    private var _typeUtils: TypeUtils? = null
    val typeUtils: TypeUtils
        get() {
            if (_typeUtils == null) {
                _typeUtils = TypeUtils(this)
            }
            return _typeUtils!!
        }

    override fun copy(): StepConfiguration {
        val newConfig = StepConfiguration(saxonConfig, context.copy(), environment)
        newConfig._inscopeStepTypes.putAll(_inscopeStepTypes)
        newConfig.validationMode = validationMode
        newConfig._stepName = _stepName
        return newConfig
    }

    fun error(message: () -> String) {
        environment.messageReporter.error { Report(Verbosity.ERROR, message(), location) }
    }

    fun warn(message: () -> String) {
        environment.messageReporter.warn { Report(Verbosity.WARN, message(), location) }
    }

    fun info(message: () -> String) {
        environment.messageReporter.info { Report(Verbosity.INFO, message(), location) }
    }

    fun debug(message: () -> String) {
        environment.messageReporter.debug { Report(Verbosity.DEBUG, message(), location) }
    }

    fun trace(message: () -> String) {
        environment.messageReporter.trace { Report(Verbosity.TRACE, message(), location) }
    }

    fun putStepType(type: QName, decl: DeclareStepInstruction) {
        _inscopeStepTypes[type] = decl
    }

    fun stepDeclaration(name: QName): DeclareStepInstruction? {
        return inscopeStepTypes[name]
    }

    fun stepAvailable(name: QName): Boolean {
        val decl = stepDeclaration(name) ?: return false
        if (decl.isAtomic) {
            return environment.atomicStepAvailable(name)
        }
        return true
    }

    override fun atomicStepAvailable(name: QName): Boolean {
        val decl = stepDeclaration(name)
        if (decl == null) {
            return environment.atomicStepAvailable(name)
        }
        return decl.isAtomic
    }

}