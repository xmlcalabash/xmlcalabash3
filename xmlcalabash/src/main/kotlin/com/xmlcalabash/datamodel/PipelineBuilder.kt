package com.xmlcalabash.datamodel

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import java.net.URI

class PipelineBuilder private constructor(val stepConfig: InstructionConfiguration) {
    companion object {
        fun newInstance(xmlCalabash: XmlCalabash): PipelineBuilder {
            return newInstance(xmlCalabash, null)
        }

        fun newInstance(xmlCalabash: XmlCalabash, version: Double?): PipelineBuilder {
            val newConfig = xmlCalabash.saxonConfiguration.newConfiguration()
            val context = DocumentContextImpl(newConfig)
            val environment = CompileEnvironment("", xmlCalabash)
            newConfig.environment = environment
            val instructionConfig
                = InstructionConfiguration(newConfig, context, environment).with("p", NsP.namespace)
            val builder = PipelineBuilder(instructionConfig)
            builder.version = version
            val library = StandardLibrary.getInstance(builder)
            environment.standardSteps.putAll(library.exportedSteps)
            for ((_, decl) in library.exportedSteps) {
                instructionConfig.addVisibleStepType(decl)
            }
            return builder
        }
    }

    private var started = false
    var version: Double? = null
    val staticOptionsManager = StaticOptionsManager()

    fun load(uri: URI): XProcDocument {
        // Irrespective of what the filename suggests, if we're trying to load a pipeline
        // force the content type to be application/xml. This is mostly so that .xpl files
        // are XML even if the local system isn't configured that way.
        val properties = DocumentProperties()
        properties.set(Ns.contentType, "application/xml")
        try {
            return stepConfig.environment.documentManager.load(uri, stepConfig, properties)
        } catch (ex: XProcException) {
            if (ex.error.code == NsErr.xd(11)) {
                throw ex.error.with(NsErr.xs(52)).exception()
            }
            throw ex
        }
    }

    fun newLibrary(): LibraryInstruction {
        for ((optname, value) in staticOptionsManager.useWhenOptions) {
            staticOptionsManager.compileTimeValue(optname, XProcExpression.constant(stepConfig, value))
        }
        val decl = LibraryInstruction(stepConfig.copy())
        if (version != null) {
            decl.version = version
        }
        decl.builder = this
        return decl
    }

    fun newDeclareStep(): DeclareStepInstruction {
        for ((optname, value) in staticOptionsManager.useWhenOptions) {
            staticOptionsManager.compileTimeValue(optname, XProcExpression.constant(stepConfig, value))
        }

        for ((optname, value) in staticOptionsManager.useWhenOptions) {
            stepConfig.addStaticBinding(optname, value)
        }

        val decl = DeclareStepInstruction(null, stepConfig.copyNew())
        if (version != null) {
            decl.version = version
        }
        decl.builder = this
        return decl
    }

    fun option(name: QName, value: XProcDocument) {
        option(name, value.value)
    }

    fun option(name: QName, value: XdmValue) {
        if (started) {
            throw stepConfig.exception(XProcError.xiTooLateForStaticOptions(name))
        }

        val curValue : XdmValue? = staticOptionsManager.useWhenOptions[name]
        if (curValue == null) {
            staticOptionsManager.useWhenValue(name, value)
        } else {
            staticOptionsManager.useWhenValue(name, curValue.append(value))
        }
    }
}