package com.xmlcalabash.ext.rdf

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import net.sf.saxon.s9api.QName

class RdfStepProvider: AtomicStepProvider, AtomicStepManager {
    companion object {
        private val cx_rdfMerge = QName(NsCx.namespace, "cx:rdf-merge")
        private val cx_rdfGraph = QName(NsCx.namespace, "cx:rdf-graph")
        private val cx_sparql = QName(NsCx.namespace, "cx:sparql")
        private val cx_rdfa = QName(NsCx.namespace, "cx:rdfa")

        private val STEPS = setOf(cx_rdfMerge, cx_rdfGraph, cx_sparql, cx_rdfa)
    }

    override fun create(): AtomicStepManager {
        return this
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        return when (params.stepType) {
            cx_rdfMerge -> { -> RdfMergeStep() }
            cx_rdfGraph -> { -> RdfGraphStep() }
            cx_sparql -> { -> SparqlStep() }
            cx_rdfa -> { -> RdfaStep() }
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