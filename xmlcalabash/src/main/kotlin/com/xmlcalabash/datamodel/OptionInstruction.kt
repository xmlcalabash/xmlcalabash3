package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.trans.SymbolicName

open class OptionInstruction(parent: XProcInstruction, name: QName, stepConfig: StepConfiguration): VariableBindingContainer(parent, name, stepConfig, NsP.option) {
    private val _values = mutableListOf<XdmAtomicValue>()
    var values: List<XdmAtomicValue>
        get() = _values
        set(value) {
            checkOpen()
            _values.clear()
            _values.addAll(value)
        }

    private var _static: Boolean = false
    var static: Boolean
        get() = _static
        set(value) {
            checkOpen()
            _static = value
        }

    private var _required: Boolean? = null
    var required: Boolean?
        get() = _required
        set(value) {
            checkOpen()
            _required = value
        }

    private var _visibility: Visibility? = null
    var visibility: Visibility?
        get() = _visibility
        set(value) {
            checkOpen()
            _visibility = value
        }

    override fun canBeResolvedStatically(): Boolean {
        return static && select?.canBeResolvedStatically() ?: false
    }

    override fun elaborateInstructions() {
        alwaysDynamic = !static
        required = required == true
        visibility = visibility ?: Visibility.PUBLIC

        if (required == true && static) {
            throw XProcError.xsRequiredAndStatic(name).exception()
        }

        if (select == null) {
            if (required == true) {
                if (!(parent as DeclareStepInstruction).isAtomic) {
                    val lexical = if (name.prefix.isNullOrEmpty()) {
                        name.localName
                    } else {
                        "${name.prefix}:${name.localName}"
                    }
                    select = XProcExpression.select(stepConfig,
                        "Q{http://xmlcalabash.com/ns/extensions}error('static', 18, Q{http://www.w3.org/2005/xpath-functions}QName('${name.namespaceUri}', '${lexical}'))",
                        SequenceType.ANY, false)
                }
            } else {
                if (static && stepConfig.staticBindings.contains(name)) {
                    select = XProcExpression.constant(stepConfig, stepConfig.staticBindings[name]!!, asType ?: SequenceType.ANY, values)
                } else {
                    select = XProcExpression.constant(stepConfig, XdmEmptySequence.getInstance(), asType ?: SequenceType.ANY, values)
                }
            }
        } else {
            if (required == true) {
                throw XProcError.xsRequiredAndDefaulted(name).exception()
            }
            asType = asType ?: stepConfig.parseSequenceType("item()*")
            select = select!!.cast(asType!!)
        }

        // If an option contains variable references, they can only be to preceding options
        for (name in select!!.variableRefs) {
            if (name !in stepConfig.inscopeVariables) {
                throw XProcError.xsXPathStaticError("There is no \$${name} option in scope").exception()
            }
        }

        if (select!!.functionRefs.isNotEmpty()) {
            val ourSet = stepConfig.saxonConfig.configuration.integratedFunctionLibrary
            val compiler = stepConfig.newXPathCompiler()
            for (func in select!!.functionRefs) {
                val sqname = StructuredQName(func.first.prefix, func.first.namespaceUri, func.first.localName)
                var found = stepConfig.saxonConfig.configuration.getSystemFunction(sqname, func.second) != null
                found = found || ourSet.getFunctionItem(SymbolicName.F(sqname, func.second), compiler.underlyingStaticContext) != null
                if (!found) {
                    throw XProcError.xsXPathStaticError(func.first).exception()
                }
            }
        }

        if (static) {
            val inScope = stepConfig.inscopeVariables[name]
            if (inScope is OptionInstruction && inScope.static) {
                throw XProcError.xsShadowStaticOption(name).exception()
            }
            select!!.computeStaticValue(stepConfig)
            stepConfig.addStaticBinding(name, select!!.staticValue!!)
        }

        super.elaborateInstructions()
    }

    override fun empty(): EmptyInstruction {
        throw XProcError.xsInvalidElement(NsP.empty).exception()
    }

    override fun document(href: XProcExpression): DocumentInstruction {
        throw XProcError.xsInvalidElement(NsP.document).exception()
    }

    override fun pipe(): PipeInstruction {
        throw XProcError.xsInvalidElement(NsP.pipe).exception()
    }

    override fun inline(documentNode: XdmNode): InlineInstruction {
        throw XProcError.xsInvalidElement(NsP.inline).exception()
    }
}