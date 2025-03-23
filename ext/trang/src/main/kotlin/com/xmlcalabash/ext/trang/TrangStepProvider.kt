package com.xmlcalabash.ext.trang

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class TrangStepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val cx_trang = QName(NsCx.namespace, "cx:trang")
        private val cx_trang_files = QName(NsCx.namespace, "cx:trang-files")

        private val STEPS = setOf(cx_trang, cx_trang_files)
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return when (params.stepType) {
            cx_trang -> { -> TrangStep() }
            cx_trang_files -> { -> TrangFilesStep() }
            else -> {
                throw XProcError.xiImpossible("Attempted to create ${params.stepType} step").exception()
            }
        }
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return stepType in STEPS
    }

    override fun stepTypes(): Set<QName> {
        return STEPS
    }
}