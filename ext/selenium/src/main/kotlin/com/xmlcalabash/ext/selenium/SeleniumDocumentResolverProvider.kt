package com.xmlcalabash.ext.selenium

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class SeleniumDocumentResolverProvider: SimpleDocumentResolverProvider (
    URI("https://xmlcalabash.com/ext/library/selenium.xpl"),
    "/com/xmlcalabash/ext/selenium.xpl"
)