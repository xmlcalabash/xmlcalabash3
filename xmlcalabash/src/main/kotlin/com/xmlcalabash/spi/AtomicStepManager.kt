package com.xmlcalabash.spi

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.runtime.parameters.StepParameters
import net.sf.saxon.s9api.QName

interface AtomicStepManager {
    fun stepTypes(): Set<QName>
    fun stepAvailable(stepType: QName): Boolean
    fun createStep(params: StepParameters): () -> XProcStep
}