package com.xmlcalabash.runtime

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.om.GroundedValue
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

open class XProcExecutionContext(val stepConfig: StepConfiguration) {
    var iterationPosition = 1L
    var iterationSize = 1L
    private val properties = mutableMapOf<GroundedValue, MutableList<XdmMap>>()

    constructor(context: XProcExecutionContext): this(context.stepConfig) {
        iterationPosition = context.iterationPosition
        iterationSize = context.iterationSize
        properties.putAll(context.properties)
        //println("Cpy XProcExecutionContext: ${context.properties.size}")
    }

    open fun stepAvailable(name: QName): Boolean {
        return stepConfig.stepAvailable(name)
    }

    fun addProperties(doc: XProcDocument) {
        val item: GroundedValue? = if (doc.value is XdmNode) {
            (doc.value as XdmNode).underlyingNode
        } else {
            doc.value.underlyingValue
        }

        if (item != null) {
            synchronized(properties) {
                if (!properties.containsKey(item)) {
                    properties[item] = mutableListOf()
                }
                //println("Add ${item} to ${this}; ${doc.properties.asMap().size}")
                properties[item]!!.add(stepConfig.typeUtils.asXdmMap(doc.properties.asMap()))
            }
        }
    }

    fun removeProperties(doc: XProcDocument) {
        val item: GroundedValue? = if (doc.value is XdmNode) {
            (doc.value as XdmNode).underlyingNode
        } else {
            doc.value.underlyingValue
        }

        if (item != null) {
            synchronized(properties) {
                if (!properties.containsKey(item)) {
                    throw RuntimeException("Configuration error: properties are missing")
                }
                val maplist = properties[item]!!
                when (maplist.size) {
                    0 -> throw RuntimeException("Configuration error: properties are missing")
                    1 -> properties.remove(item)
                    else -> maplist.removeLast()
                }
            }
        }
    }

    fun getProperties(item: GroundedValue): XdmMap? {
        synchronized(properties) {
            if (properties.containsKey(item)) {
                return properties[item]!!.last()
            }
        }
        return null
    }
}
