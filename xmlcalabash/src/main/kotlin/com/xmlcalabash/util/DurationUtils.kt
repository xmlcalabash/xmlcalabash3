package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.value.DayTimeDurationValue
import java.time.Duration

class DurationUtils {
    companion object {
        fun parseDuration(stepConfig: XProcStepConfiguration, str: String): Duration {
            var xdtStr = ""
            try {
                val s = str.toLong()
                if (s < 0) {
                    throw stepConfig.exception(XProcError.xdBadType("Invalid duration: ${str}"))
                }
                xdtStr = "PT%dS".format(s)
            } catch (_: NumberFormatException) {
                try {
                    val s = str.toDouble()
                    if (s < 0) {
                        throw stepConfig.exception(XProcError.xdBadType("Invalid duration: ${str}"))
                    }
                    xdtStr = "PT%1.2fS".format(s)
                } catch (_: NumberFormatException) {
                    xdtStr = str
                }
            }

            val xdt = try {
                val compiler = stepConfig.newXPathCompiler()
                val exec = compiler.compile("Q{${NsXs.namespace}}dayTimeDuration(\"${xdtStr}\")")
                val selector = exec.load()
                selector.evaluate()
            } catch (ex: Exception) {
                throw stepConfig.exception(XProcError.xdBadType("Invalid duration: ${str}"), ex)
            }

            return (xdt.underlyingValue as DayTimeDurationValue).toJavaDuration()
        }

        fun prettyPrint(duration: Duration): String {
            val sb = StringBuilder()
            val days = duration.toDaysPart()
            if (days > 0) {
                sb.append("${days} day${if (days != 1L) "s" else ""}, ")
            }
            val hours = duration.toHoursPart()
            if (hours > 0) {
                sb.append("${hours} hour${if (hours != 1) "s" else ""}, ")
            }
            val minutes = duration.toMinutesPart()
            if (minutes > 0) {
                sb.append("${minutes} minute${if (minutes != 1) "s" else ""}, ")
            }
            val seconds = duration.toSecondsPart()
            val ms = duration.toMillisPart()
            val frac = if (ms > 0) {
                val fs = "%1.2f".format(ms / 1000.0)
                fs.substring(1)
            } else {
                ""
            }

            sb.append("${seconds}${frac} second${if (seconds != 1 || frac != "") "s" else ""}")

            return sb.toString()
        }
    }
}