package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.str.BMPString
import net.sf.saxon.type.ValidationFailure
import net.sf.saxon.value.DayTimeDurationValue
import net.sf.saxon.value.DurationValue
import org.apache.logging.log4j.kotlin.logger

open class SleepStep(): AbstractAtomicStep() {
    val cache = mutableListOf<XProcDocument>()

    override fun input(port: String, doc: XProcDocument) {
        cache.add(doc)
    }

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

        while (cache.isNotEmpty()) {
            receiver.output("result", cache.removeFirst());
        }
    }

    override fun toString(): String = "p:sleep"
}