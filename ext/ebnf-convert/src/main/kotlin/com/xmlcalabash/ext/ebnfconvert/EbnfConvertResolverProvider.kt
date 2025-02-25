package com.xmlcalabash.ext.ebnfconvert

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class EbnfConvertResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/ebnf-convert.xpl"),
    "/com/xmlcalabash/ext/ebnf-convert.xpl")