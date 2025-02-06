package com.xmlcalabash.ext.metadataextractor

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class MetadataExtractorStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:metadata-extractor"),
    { -> MetadataExtractor() })