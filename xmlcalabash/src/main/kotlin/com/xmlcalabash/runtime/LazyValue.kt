package com.xmlcalabash.runtime

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.datamodel.XProcExpression
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.datamodel.DocumentContext
import net.sf.saxon.s9api.XdmValue

/**
 * Holds a value that will be evaluated lazily.
 *
 * This slightly awkward construction is necessary because we don't want to evaluate
 * options that aren't used and we can't construct an XProcConstantExpression at
 * runtime.
 */
class LazyValue private constructor(val context: DocumentContext, config: StepConfiguration) {
    private var expression: XProcExpression? = null
    private var constant: XProcDocument? = null
    private val resolvedConfig = config.copy()

    init {
        resolvedConfig.updateWith(context.inscopeNamespaces)
    }

    constructor(context: DocumentContext, expression: XProcExpression, config: StepConfiguration): this(context, config) {
        this.expression = expression
    }

    constructor(doc: XProcDocument, config: StepConfiguration): this(doc.context, config) {
        this.constant = doc
    }

    val value: XdmValue by lazy {
        if (constant != null) {
            constant!!.value
        } else {
            expression!!.evaluate(resolvedConfig)
        }
    }
}