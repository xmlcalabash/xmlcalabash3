package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode

open class JsonJoinStep(): AbstractAtomicStep() {
    val inputs = mutableListOf<XProcDocument>()

    override fun input(port: String, doc: XProcDocument) {
        inputs.add(doc)
    }

    override fun run() {
        super.run()

        val flatten = stringBinding(Ns.flattenToDepth)
        val depth = when (flatten) {
            null -> 0
            "unbounded" -> Integer.MAX_VALUE
            else -> {
                var intval = 0
                try {
                    intval = flatten.toInt()
                } catch (ex: NumberFormatException) {
                    throw XProcError.xcInvalidFlatten(flatten).exception()
                }
                if (intval < 0) {
                    throw XProcError.xcInvalidFlatten(flatten).exception()
                }
                intval
            }
        }

        var value = XdmArray()
        for (input in inputs) {
            when (input.value) {
                is XdmMap -> value = value.addMember(input.value)
                is XdmAtomicValue -> value = value.addMember(input.value)
                is XdmArray -> value = addArray(value, input.value as XdmArray, depth)
                is XdmNode -> value = value.addMember(input.value)
                else -> throw XProcError.xcUnsupportedForJoin().exception()
            }
        }

        receiver.output("result", XProcDocument.ofJson(value, stepConfig))
    }

    private fun addArray(value: XdmArray, array: XdmArray, depth: Int): XdmArray {
        if (depth > 0) {
            var newValue = value
            for (item in array.asList()) {
                when (item) {
                    is XdmMap -> newValue = newValue.addMember(item)
                    is XdmAtomicValue -> newValue = newValue.addMember(item)
                    is XdmArray -> newValue = addArray(newValue, item, depth - 1)
                    is XdmNode -> newValue = newValue.addMember(XdmAtomicValue(item.stringValue))
                    else -> throw XProcError.xcUnsupportedForJoin().exception()
                }
            }
            return newValue
        } else {
            return value.addMember(array)
        }
    }

    override fun toString(): String = "p:json-join"
}