package com.xmlcalabash.ext.asciidoctor

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class AsciidoctorDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/asciidoctor.xpl"),
    "/com/xmlcalabash/ext/asciidoctor.xpl")