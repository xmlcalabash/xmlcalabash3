package com.xmlcalabash.util

import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.Configuration
import net.sf.saxon.lib.SchemaURIResolver
import javax.xml.transform.stream.StreamSource

class XsdResolver(val stepConfig: XProcStepConfiguration): SchemaURIResolver {
    override fun setConfiguration(p0: Configuration?) {
        // I don't care???
    }

    override fun resolve(moduleURI: String?, baseURI: String?, locations: Array<out String>?): Array<StreamSource>? {
        return stepConfig.environment.documentManager.resolve(moduleURI, baseURI, locations)
    }
}