package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.AtomicExpressionStepInstruction
import com.xmlcalabash.datamodel.OptionInstruction
import com.xmlcalabash.datamodel.StaticOptionDetails
import com.xmlcalabash.datamodel.WithOptionInstruction
import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue

class ModelOption private constructor(val parent: Model, val name: QName, val required: Boolean) {
    private var _static = false
    val static: Boolean
        get() = _static

    private var _staticValue: XProcExpression? = null
    val staticValue: XProcExpression?
        get() = _staticValue

    private lateinit var _asType: SequenceType
    val asType: SequenceType
        get() = _asType

    private lateinit var _values: List<XdmAtomicValue>
    val values: List<XdmAtomicValue>
        get() = _values

    constructor(parent: Model, option: OptionInstruction): this(parent, option.name, option.required == true) {
        _static = option.static
        _asType = option.asType!!
        _values = option.values
        if (option.canBeResolvedStatically()) {
            _staticValue = option.select!!
        }
    }

    constructor(parent: Model, option: WithOptionInstruction): this(parent, option.name, false) {
        _static = false
        _asType = option.asType!!
        _values = option.optionValues
        if (option.canBeResolvedStatically()) {
            _staticValue = option.select!!
        }
    }

    constructor(parent: Model, option: StaticOptionDetails): this(parent, option.name, false) {
        _static = true
        _asType = option.asType
        _values = option.values
        _staticValue = option.staticValue
    }

    constructor(parent: Model, option: AtomicExpressionStepInstruction): this(parent, option.bindingName, true) {
        _static = false
        _asType = SequenceType.ANY
        _values = emptyList()
    }
}