package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import org.apache.logging.log4j.kotlin.logger

open class SleepStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val duration = stringBinding(Ns.duration)!!
        try {
            val ms = duration.toLong()
            if (ms < 0) {
                throw stepConfig.exception(XProcError.xdBadType("Invalid duration: ${duration}"))
            }

            logger.debug { "Waiting for ${String.format("%1.1f", ms)}s ... "}
            Thread.sleep(ms)
        } catch (ex: NumberFormatException) {
            throw stepConfig.exception(XProcError.xdBadType("Invalid duration: ${duration}"), ex)
        }

        for (doc in queues["source"]!!) {
            receiver.output("result", doc)
        }
    }

    override fun toString(): String = "p:sleep"
}