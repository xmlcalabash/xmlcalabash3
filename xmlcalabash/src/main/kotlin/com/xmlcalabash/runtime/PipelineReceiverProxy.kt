package com.xmlcalabash.runtime

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.steps.Consumer

class PipelineReceiverProxy(val receiver: Receiver): Consumer {
    override val id = "pipeline-receiver"

    override fun input(port: String, doc: XProcDocument) {
        receiver.output(port, doc)
    }

    override fun close(port: String) {
        // nop
    }
}