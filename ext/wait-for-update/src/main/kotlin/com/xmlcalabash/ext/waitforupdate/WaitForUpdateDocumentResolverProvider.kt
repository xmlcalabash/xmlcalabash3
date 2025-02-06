package com.xmlcalabash.ext.waitforupdate

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class WaitForUpdateDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/wait-for-update.xpl"),
    "/com/xmlcalabash/ext/wait-for-update.xpl")