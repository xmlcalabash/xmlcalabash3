package com.xmlcalabash.documents

import net.sf.saxon.s9api.XdmValue

data class LazyDocumentValue(val value: XdmValue, val properties: DocumentProperties)