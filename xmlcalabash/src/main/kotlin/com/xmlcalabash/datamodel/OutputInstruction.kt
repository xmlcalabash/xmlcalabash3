package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsXs
import net.sf.saxon.s9api.XdmMap

class OutputInstruction(parent: XProcInstruction): PortBindingContainer(parent, parent.stepConfig.copy(), NsP.output) {
    constructor(parent: XProcInstruction, port: String, primary: Boolean?, sequence: Boolean?) : this(parent) {
        this._port = port
        this._primary = primary
        this._sequence = sequence
    }

    private var _serialization: XProcExpression? = null
    var serialization: XProcExpression
        get() = _serialization!!
        set(value) {
            checkOpen()
            _serialization = value.cast(parent!!.stepConfig.qnameMapType)
        }

    override fun elaborateInstructions() {
        if (select != null) {
            throw XProcError.xsAttributeForbidden(Ns.select).exception()
        }

        if (_serialization == null) {
            _serialization = XProcExpression.constant(stepConfig, XdmMap(), stepConfig.qnameMapType)
        }

        super.elaborateInstructions()
    }
}