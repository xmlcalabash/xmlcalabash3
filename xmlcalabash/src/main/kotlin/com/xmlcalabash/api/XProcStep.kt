package com.xmlcalabash.api

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import net.sf.saxon.s9api.QName

interface XProcStep {
    fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters)
    fun extensionAttributes(attributes: Map<QName, String>)

    fun option(name: QName, binding: LazyValue)
    fun inScopeBinding(name: QName, binding: LazyValue)
    fun input(port: String, doc: XProcDocument)
    fun run()

    fun reset()
    fun teardown()
}