package com.xmlcalabash.steps

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.parameters.StepParameters
import kotlin.math.max

open class TextTailStep(): AbstractTextStep() {
    override fun run() {
        super.run()

        val count = integerBinding(Ns.count)!!
        val lines = textLines()
        val sb = StringBuilder()

        if (count == 0) {
            receiver.output("result", source)
            return
        }

        val fromTo = if (count < 0) {
            Pair(0, lines.size + count)
        } else {
            Pair(max(0, lines.size - count), lines.size)
        }

        for (index in fromTo.first ..< fromTo.second) {
            sb.append(lines[index]).append("\n")
        }

        val result = source.with(textNode(sb.toString(), source.baseURI))
        receiver.output("result", result)
    }

    override fun toString(): String = "p:text-count"
}