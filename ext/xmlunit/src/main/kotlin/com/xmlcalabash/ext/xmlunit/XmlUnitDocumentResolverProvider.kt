package com.xmlcalabash.ext.xmlunit

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class XmlUnitDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/xmlunit.xpl"),
    "/com/xmlcalabash/ext/xmlunit.xpl")