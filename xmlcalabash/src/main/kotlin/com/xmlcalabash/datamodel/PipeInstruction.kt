package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import java.lang.RuntimeException

class PipeInstruction(parent: XProcInstruction): ConnectionInstruction(parent, NsP.pipe) {
    private var _readablePort: PortBindingContainer? = null
    val readablePort: PortBindingContainer?
        get() = _readablePort

    private var _implicit: Boolean? = null
    val implicit: Boolean
        get() = _implicit ?: false

    constructor(parent: XProcInstruction, step: String?, port: String?): this(parent) {
        this.step = step
        this.port = port
    }

    private var _step: String? = null
    var step: String?
        get() = _step
        set(value) {
            checkOpen()
            _step = value
        }

    private var _port: String? = null
    var port: String?
        get() = _port
        set(value) {
            checkOpen()
            _port = value
        }

    override fun elaborateInstructions() {
        val fromStep = if (step == null) {
            if (stepConfig.drp == null) {
                throw stepConfig.exception(XProcError.xsNoStepPortNotReadable())
            }
            _implicit = true
            stepConfig.drp!!.parent as StepDeclaration
        } else {
            var decl = stepConfig.inscopeStepNames[step]
            if (decl == null) {
                var p: XProcInstruction? = parent
                while (p != null) {
                    if (p is StepDeclaration) {
                        decl = p.stepConfig.inscopeStepNames[step]
                        if (decl != null) {
                            break
                        }
                    }
                    p = p.parent
                }
            }

            if (decl == null) {
                if (readablePort == null) {
                    throw stepConfig.exception(XProcError.xsPortNotReadable(step!!))
                }

                // If we're in the middle of rewriting things, the step might not
                // be in our ancestors yet.
                decl = readablePort!!.parent as StepDeclaration
            }

            decl
        }

        // Special cases. If the decl is a when, otherwise, catch, or finally,
        // we must not be inside one of it's siblings
        if (fromStep is WhenInstruction || fromStep is OtherwiseInstruction
            || fromStep is CatchInstruction || fromStep is FinallyInstruction) {
            var p: XProcInstruction? = this
            while (p != null) {
                if (p === fromStep) {
                    break
                }
                if (p is WhenInstruction || p is OtherwiseInstruction
                    || p is CatchInstruction || p is FinallyInstruction) {
                    val pp = p.parent
                    if (pp != null && pp.children.contains(fromStep)) {
                        if (step != null && port != null) {
                            throw stepConfig.exception(XProcError.xsPortNotReadable(step!!, port!!))
                        } else if (step != null) {
                            throw stepConfig.exception(XProcError.xsPortNotReadable(step!!))
                        } else {
                            throw stepConfig.exception(XProcError.xsNoStepPortNotReadable()) // I don't think this is actualy possible
                        }
                    }
                }
                p = p.parent
            }
        }

        _step = fromStep.name

        val fromPort = if (hasAncestor(fromStep)) {
            if (port == null) {
                fromStep.primaryInput()
            } else {
                fromStep.namedInput(port!!)
            }
        } else {
            if (port == null) {
                fromStep.primaryOutput()
            } else {
                fromStep.namedOutput(port!!)
            }
        }

        if (fromPort == null) {
            if (port == null) {
                throw stepConfig.exception(XProcError.xsNoPortPortNotReadable())
            } else {
                throw stepConfig.exception(XProcError.xsPortNotReadable(port!!))
            }
        } else {
            _port = fromPort.port
            _readablePort = fromPort
        }

        if (fromStep !is CompoundStepDeclaration && fromStep == parent?.parent) {
            throw stepConfig.exception(XProcError.xsOutputPortNotReadable(step ?: "", port ?: ""))
        }

        super.elaborateInstructions()
    }

    fun setReadablePort(readable: PortBindingContainer, implicit: Boolean) {
        _readablePort = readable
        step = (readable.parent!! as StepDeclaration).name
        port = readable.port
        _implicit = implicit
    }

    override fun promoteToStep(parent: StepDeclaration, step: StepDeclaration): List<AtomicStepInstruction> {
        // Pipes don't get promoted
        return emptyList()
    }

    override fun toString(): String {
        return "${instructionType}/${id} step=${step ?: ""} port=${port ?: ""}"
    }
}