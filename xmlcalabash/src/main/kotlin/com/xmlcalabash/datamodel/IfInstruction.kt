package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName

class IfInstruction(parent: XProcInstruction): ChooseInstruction(parent, NsP.`if`) {
    override val contentModel = mapOf(NsP.withInput to '1', NsP.`when` to '*', NsP.otherwise to '?')
    private val whenInstruction = WhenInstruction(this)

    init {
        _children.add(whenInstruction)
    }

    var test: String
        get() {
            return whenInstruction.test
        }
        set(value) {
            checkOpen()
            whenInstruction.test = value
        }

    var collection: Boolean?
        get() {
            return whenInstruction.collection
        }
        set(value) {
            checkOpen()
            whenInstruction.collection = value
        }

    override fun elaborateInstructions() {
        super.elaborateInstructions()
        for (output in children.filterIsInstance<OutputInstruction>()) {
            if (output.primary == true) {
                return
            }
        }
        throw stepConfig.exception(XProcError.xsPrimaryOutputRequiredOnIf())
    }

    override fun output(port: String?): OutputInstruction {
        return whenInstruction.output(port)
    }

    override fun output(port: String, primary: Boolean?, sequence: Boolean): OutputInstruction {
        return whenInstruction.output(port, primary, sequence)
    }

    override fun forEach(): ForEachInstruction {
        return whenInstruction.forEach()
    }

    override fun viewport(): ViewportInstruction {
        return whenInstruction.viewport()
    }

    override fun choose(): ChooseInstruction {
        return whenInstruction.choose()
    }

    override fun ifInstruction(): IfInstruction {
        return whenInstruction.ifInstruction()
    }

    override fun group(): GroupInstruction {
        return whenInstruction.group()
    }

    override fun tryInstruction(): TryInstruction {
        return whenInstruction.tryInstruction()
    }

    override fun atomicStep(type: QName): AtomicStepInstruction {
        return whenInstruction.atomicStep(type)
    }

}