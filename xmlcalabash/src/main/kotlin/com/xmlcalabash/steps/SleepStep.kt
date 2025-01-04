package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.DurationUtils
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.value.DayTimeDurationValue
import java.time.Duration

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