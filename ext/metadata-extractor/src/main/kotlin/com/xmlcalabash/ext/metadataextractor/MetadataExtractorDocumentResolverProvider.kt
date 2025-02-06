package com.xmlcalabash.ext.metadataextractor

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class MetadataExtractorDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/metadata-extractor.xpl"),
    "/com/xmlcalabash/ext/metadata-extractor.xpl")