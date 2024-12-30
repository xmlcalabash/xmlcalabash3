package com.xmlcalabash.xvrl

import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class XvrlProvenance private constructor(stepConfiguration: XProcStepConfiguration): XvrlElement(stepConfiguration) {
    val locations = mutableListOf<XvrlLocation>()

    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, attr: Map<QName,String> = emptyMap()): XvrlProvenance {
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
        builder.addStartElement(NsXvrl.provenance, stepConfig.attributeMap(attributes))
        for (location in locations) {
            location.serialize(builder)
        }
        builder.addEndElement()
    }
}