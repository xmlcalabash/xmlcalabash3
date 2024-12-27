package com.xmlcalabash.runtime

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.steps.Consumer
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.s9api.XdmMap

class PipelineReceiverProxy(val receiver: Receiver): Consumer {
    override val id = "pipeline-receiver"
    private var _serialization = mutableMapOf<String, XdmMap>()

    fun serialization(port: String, properties: XdmMap) {
        _serialization[port] = properties
    }

    override fun input(port: String, doc: XProcDocument) {
        // Merge the serialization properties of the document with the output port.
        // The port wins.
        val docprop = doc.properties.getSerialization()
        if ((docprop.underlyingValue as MapItem).isEmpty) {
            doc.properties.setSerialization(_serialization[port] ?: XdmMap())
        } else {
            var newmap = doc.properties.getSerialization()
            val portmap = _serialization[port] ?: XdmMap()
            for (key in portmap.keySet()) {
                newmap = newmap.put(key, portmap[key])
            }
            doc.properties.setSerialization(newmap)
        }
        receiver.output(port, doc)
    }

    override fun close(port: String) {
        // nop
    }
}