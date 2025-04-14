package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.InstructionConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.XdmNode

abstract class RootNode(parent: AnyNode?, parentConfig: InstructionConfiguration, node: XdmNode): ElementNode(parent, parentConfig, node) {
    var firstPass = true

    internal open fun resolve(manager: XplDocumentManager, context: UseWhenContext) {
        var done = false
        val localContext = context.copy()

        while (!done) {
            done = true

            if (this is DeclareStepNode) {
                if (type != null) {
                    val impl = if (useWhen == null) {
                        StepImplementation(false, { false })
                    } else {
                        StepImplementation(true, { (useWhen == true) && (!isAtomic || stepConfig.atomicStepAvailable(type!!)) })
                    }
                    localContext.stepTypes[type!!] = impl
                }
            }

            for (child in children.filterIsInstance<DeclareStepNode>()) {
                if (child.type != null) {
                    val impl = if (child.useWhen == null) {
                        StepImplementation(false, { false })
                    } else {
                        StepImplementation(true, { (child.useWhen == true) && (!child.isAtomic || stepConfig.atomicStepAvailable(child.type!!)) })
                    }
                    localContext.stepTypes[child.type!!] = impl
                }
            }


            for (import in children.filterIsInstance<ImportNode>()) {
                if (import.useWhen == true) {
                    val doc = try {
                        manager.load(import.href, localContext)
                    } catch (ex: Exception) {
                        if (ex is XProcException) {
                            throw ex
                        }
                        throw XProcError.xsNotImportable(import.href).exception(ex)
                    }
                    for ((name, impl) in doc.exportedStepTypes) {
                        localContext.stepTypes[name] = impl
                    }
                    for ((name, value) in doc.exportedStaticOptions) {
                        localContext.staticOptions[name] = value
                    }
                }
                if (import.useWhen == null) {
                    localContext.unknownStepTypes = true
                }
            }

            resolveUseWhen(localContext)

            if (localContext.resolvedCount > 0 && localContext.useWhen.isNotEmpty()) {
                done = false
            } else {
                if (firstPass) {
                    firstPass = false
                    done = false
                } else {
                    context.useWhen.addAll(localContext.useWhen)
                    context.staticOptions.putAll(localContext.staticOptions)
                }
            }
        }
    }
}
