package com.xmlcalabash.runtime

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.datamodel.InstructionConfiguration
import com.xmlcalabash.datamodel.DocumentContext

class XProcStepConfiguration(saxonConfig: SaxonConfiguration,
                             context: DocumentContext,
                             environment: RuntimeEnvironment): StepConfiguration(saxonConfig, context, environment) {
    fun from(config: InstructionConfiguration, newConfig: Boolean = true): XProcStepConfiguration {
        val xconfig = if (newConfig) {
            val newSaxonConfig = config.saxonConfig.newConfiguration()
            XProcStepConfiguration(newSaxonConfig, config.context.copy(newSaxonConfig), environment as RuntimeEnvironment)
        } else {
            XProcStepConfiguration(config.saxonConfig, config.context, environment as RuntimeEnvironment)
        }
        xconfig._inscopeStepTypes.putAll(config.inscopeStepTypes)

        return xconfig
    }

    override fun copy(): XProcStepConfiguration {
        val xconfig = XProcStepConfiguration(saxonConfig, context, environment as RuntimeEnvironment)
        xconfig._inscopeStepTypes.putAll(inscopeStepTypes)
        return xconfig
    }

    override fun copy(newConfig: SaxonConfiguration): XProcStepConfiguration {
        val xconfig = XProcStepConfiguration(newConfig, context.copy(newConfig), environment as RuntimeEnvironment)
        xconfig._inscopeStepTypes.putAll(inscopeStepTypes)
        return xconfig
    }
}