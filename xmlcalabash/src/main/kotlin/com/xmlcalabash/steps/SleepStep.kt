package com.xmlcalabash.steps

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.DurationUtils

open class SleepStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val duration = DurationUtils.parseDuration(stepConfig, stringBinding(Ns.duration)!!)
        val ms = duration.toMillis()

        stepConfig.debug { "Sleeping for ${DurationUtils.prettyPrint(duration)} ... "}
        Thread.sleep(ms)

        for (doc in queues["source"]!!) {
            receiver.output("result", doc)
        }
    }


    override fun toString(): String = "p:sleep"
}