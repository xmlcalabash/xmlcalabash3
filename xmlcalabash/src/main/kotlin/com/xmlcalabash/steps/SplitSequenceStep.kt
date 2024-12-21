package com.xmlcalabash.steps

import com.xmlcalabash.namespace.Ns
import net.sf.saxon.om.Item
import net.sf.saxon.tree.iter.ManualIterator

open class SplitSequenceStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val documents = queues["source"]!!
        val test = stringBinding(Ns.test)!!
        val testContext = options[Ns.test]!!.context
        val initialOnly = booleanBinding(Ns.initialOnly) ?: false
        var shunt = false
        var index = 0

        for (doc in documents) {
            index++
            if (shunt) {
                receiver.output("not-matched", doc)
                continue
            }

            val compiler = stepConfig.processor.newXPathCompiler()
            compiler.baseURI = doc.baseURI
            for ((prefix, uri) in testContext.inscopeNamespaces) {
                compiler.declareNamespace(prefix, uri.toString())
            }
            val exec = compiler.compile(test)
            val select = exec.load()
            select.resourceResolver = stepConfig.environment.documentManager

            val dyncontext = select.underlyingXPathContext
            val context = dyncontext.xPathContextObject

            val fakeIterator = ManualIterator(doc.value.underlyingValue as Item, index)
            fakeIterator.setLengthFinder { -> documents.size }
            context.currentIterator = fakeIterator

            if (select.effectiveBooleanValue()) {
                receiver.output("matched", doc)
            } else {
                receiver.output("not-matched", doc)
                shunt = initialOnly
            }
        }
    }

    override fun toString(): String = "p:split-sequence"
}