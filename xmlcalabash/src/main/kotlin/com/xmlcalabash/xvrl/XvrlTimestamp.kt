package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class XvrlTimestamp private constructor(stepConfiguration: StepConfiguration, val timestamp: ZonedDateTime): XvrlElement(stepConfiguration) {
    companion object {
        fun newInstance(stepConfig: StepConfiguration, stamp: ZonedDateTime, attr: Map<QName,String?> = emptyMap()): XvrlTimestamp {
            val timestamp = XvrlTimestamp(stepConfig, stamp)
            timestamp.commonAttributes(attr)
            return timestamp
        }
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.timestamp, stepConfig.typeUtils.attributeMap(attributes))
        builder.addText(DateTimeFormatter.ISO_INSTANT.format(timestamp))
        builder.addEndElement()
    }
}