package com.xmlcalabash.xvrl

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import java.net.URI

class XvrlProvenance private constructor(stepConfiguration: StepConfiguration): XvrlElement(stepConfiguration) {
    val locations = mutableListOf<XvrlLocation>()

    companion object {
        fun newInstance(stepConfig: StepConfiguration, attr: Map<QName,String?> = emptyMap()): XvrlProvenance {
            val provenance = XvrlProvenance(stepConfig)
            provenance.setAttributes(attr)
            return provenance
        }
    }

    // ============================================================

    fun location(location: XvrlLocation): XvrlLocation {
        locations.add(location)
        return location
    }

    fun location(href: URI?): XvrlLocation {
        val location = XvrlLocation.newInstance(stepConfig, href)
        locations.add(location)
        return location
    }

    fun location(href: URI?, line: Int, column: Int = 0): XvrlLocation {
        val location = XvrlLocation.newInstance(stepConfig, href, line, column)
        locations.add(location)
        return location
    }

    fun location(href: URI?, offset: Int): XvrlLocation {
        val location = XvrlLocation.newInstance(stepConfig, href, offset)
        locations.add(location)
        return location
    }

    // ============================================================

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.provenance, stepConfig.typeUtils.attributeMap(attributes))
        for (location in locations) {
            location.serialize(builder)
        }
        builder.addEndElement()
    }
}