package com.xmlcalabash.runtime

import com.xmlcalabash.config.ExecutionContext
import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.steps.RuntimeStepStaticContext
import com.xmlcalabash.util.SaxonValueConverter
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.s9api.*
import java.net.URI

class RuntimeStepConfiguration(
    val staticContext: RuntimeStepStaticContextImpl,
    val rteContext: RuntimeExecutionContext,
    val converter: SaxonValueConverter
): XProcStepConfiguration, RuntimeStepStaticContext by staticContext, ValueConverter by converter, ExecutionContext by rteContext {

    private var _stepName: String = ""
    val stepName: String
        get() = _stepName

    fun newInstance(stepcfg: StepConfiguration): RuntimeStepConfiguration {
        val newContext = RuntimeStepStaticContextImpl(stepcfg)
        val config = RuntimeStepConfiguration(newContext, rteContext, converter)
        config._stepName = stepcfg.stepName
        return config
    }

    fun copy(): RuntimeStepConfiguration {
        return RuntimeStepConfiguration(staticContext, rteContext, converter)
    }

    fun with(newLocation: Location): RuntimeStepConfiguration {
        val config = RuntimeStepConfiguration(staticContext.with(location), rteContext, converter)
        config._stepName = stepName
        return config
    }

    override fun uniqueUri(base: String): URI {
        return rteContext.uniqueUri(base)
    }

    override fun parseQName(name: String): QName {
        return converter.parseQName(name, inscopeNamespaces)
    }

    override fun checkType(
        varName: QName?,
        value: XdmValue,
        sequenceType: SequenceType?,
        values: List<XdmAtomicValue>
    ): XdmValue {
        return converter.checkType(varName, value, sequenceType, inscopeNamespaces, values)
    }

    override fun forceQNameKeys(inputMap: MapItem): XdmMap {
        return converter.forceQNameKeys(inputMap, inscopeNamespaces)
    }

    override fun forceQNameKeys(inputMap: XdmMap): XdmMap {
        try {
            return converter.forceQNameKeys(inputMap, inscopeNamespaces)
        } catch (ex: Exception) {
            throw XProcError.xdInvalidSerialization().exception(ex)
        }
    }
}