package com.xmlcalabash.datamodel

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.XdmNode

class InputInstruction(parent: XProcInstruction): InputBindingInstruction(parent, parent.stepConfig.copy(), NsP.input) {

    internal constructor(parent: XProcInstruction, port: String, primary: Boolean?, sequence: Boolean?) : this(parent) {
        this._port = port
        this._primary = primary
        this._sequence = sequence
    }

    override var select: XProcExpression?
        get() = _select
        set(value) {
            checkOpen()
            if (value == null) {
                _select = null
            } else {
                _select = value.cast(parent!!.stepConfig.parseSequenceType("item()*"))
            }
            /*
            if (parent is DeclareStepInstruction) {
                defaultSelect = _select
            }
             */
        }

    override fun elaborateInstructions() {
        if (!portDefined) {
            throw XProcError.xsMissingRequiredAttribute(Ns.port).exception()
        }

        if (pipe != null) {
            throw XProcError.xsAttributeForbidden(Ns.pipe).exception()
        }

        for (child in defaultBindings) {
            child.elaborateInstructions()
        }

        super.elaborateInstructions()
    }

    // ============================================================

    override fun empty(): EmptyInstruction {
        val empty = super.empty()
        if (parent is DeclareStepInstruction) {
            _children.remove(empty)
            defaultBindings.add(empty)
        }
        return empty
    }

    override fun document(href: XProcExpression): DocumentInstruction {
        val doc = super.document(href)
        if (parent is DeclareStepInstruction) {
            _children.remove(doc)
            defaultBindings.add(doc)
        }
        return doc
    }

    override fun pipe(): PipeInstruction {
        throw XProcError.xsInvalidElement(NsP.pipe).exception()
    }

    override fun pipe(step: String?, port: String?): PipeInstruction {
        throw XProcError.xsInvalidElement(NsP.pipe).exception()
    }

    override fun pipe(input: InputInstruction): PipeInstruction {
        throw XProcError.xsInvalidElement(NsP.pipe).exception()
    }

    override fun pipe(step: StepDeclaration, port: String?): PipeInstruction {
        throw XProcError.xsInvalidElement(NsP.pipe).exception()
    }

    override fun inline(document: XProcDocument): InlineInstruction {
        val inline = super.inline(document)
        if (parent is DeclareStepInstruction) {
            _children.remove(inline)
            defaultBindings.add(inline)
        }
        return inline
    }

    override fun inline(documentNode: XdmNode): InlineInstruction {
        val inline = super.inline(documentNode)
        if (parent is DeclareStepInstruction) {
            _children.remove(inline)
            defaultBindings.add(inline)
        }
        return inline
    }

}