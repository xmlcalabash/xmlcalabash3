package com.xmlcalabash.ext.jsonpatch

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class JsonPatchDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/json-patch.xpl"),
    "/com/xmlcalabash/ext/json-patch.xpl")