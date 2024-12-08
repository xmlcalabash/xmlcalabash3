package com.xmlcalabash.datamodel

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

open class BindingContainer(parent: XProcInstruction, stepConfig: InstructionConfiguration, instructionType: QName): XProcInstruction(parent, stepConfig.copy(), instructionType) {
    internal fun promoteHref(href: XProcExpression) {
        val doc = DocumentInstruction(this, href)
        _children.add(doc)
    }

    internal fun promotePipe(pipe: String) {
        if (pipe.trim().isEmpty()) {
            // Default port on default step; i.e., the drp
            _children.add(PipeInstruction(this, null, null))
            return
        }

        for (token in pipe.split("\\s+".toRegex())) {
            val pos = token.indexOf('@')
            if (pos >= 0) {
                val port = token.substring(0, pos)
                val step = token.substring(pos + 1)

                if (step.isEmpty()) {
                    throw XProcError.xsInvalidPipeAttribute(token).exception()
                }

                if (port.isEmpty()) {
                    _children.add(PipeInstruction(this, step, null))
                } else if (step.isEmpty()) {
                    _children.add(PipeInstruction(this, null, port))
                } else {
                    _children.add(PipeInstruction(this, step, port))
                }
            } else {
                _children.add(PipeInstruction(this, null, token))
            }
        }
    }

    // ============================================================

    open fun empty(): EmptyInstruction {
        val empty = EmptyInstruction(this)
        _children.add(empty)
        return empty
    }

    open fun document(href: XProcExpression): DocumentInstruction {
        val doc = DocumentInstruction(this, href)
        _children.add(doc)
        return doc
    }

    open fun pipe(): PipeInstruction {
        val pipe = PipeInstruction(this)
        _children.add(pipe)
        return pipe
    }

    open fun pipe(step: String?, port: String? = null): PipeInstruction {
        val pipe = PipeInstruction(this)
        pipe.step = step
        pipe.port = port
        _children.add(pipe)
        return pipe
    }

    open fun pipe(input: InputInstruction): PipeInstruction {
        val pipe = PipeInstruction(this)
        pipe.setReadablePort(input)
        _children.add(pipe)
        return pipe
    }

    open fun pipe(step: StepDeclaration, port: String? = null): PipeInstruction {
        val pipe = PipeInstruction(this)
        pipe.step = step.name
        pipe.port = port
        _children.add(pipe)
        return pipe
    }

    open fun inline(document: XProcDocument): InlineInstruction {
        val inline = InlineInstruction(this, document.value as XdmNode)
        _children.add(inline)
        return inline
    }

    open fun inline(documentNode: XdmNode): InlineInstruction {
        val inline = InlineInstruction(this, documentNode)
        _children.add(inline)
        return inline
    }
}