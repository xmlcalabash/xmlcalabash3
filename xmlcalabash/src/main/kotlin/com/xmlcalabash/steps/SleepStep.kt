package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import org.apache.logging.log4j.kotlin.logger

open class SleepStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val duration = stringBinding(Ns.duration)!!
        val ms = try {
            duration.toLong()
        } catch (ex: NumberFormatException) {
            throw stepConfig.exception(XProcError.xdBadType("Invalid duration: ${duration}"), ex)
        }

        if (ms < 0) {
            throw stepConfig.exception(XProcError.xdBadType("Invalid duration: ${duration}"))
        }

        stepConfig.debug { "Sleeping for ${String.format("%1.1f", (1.0 * ms) / 1000.0)}s ... "}
        Thread.sleep(ms)

        for (doc in queues["source"]!!) {
            receiver.output("result", doc)
        }
    }

    override fun toString(): String = "p:sleep"
}