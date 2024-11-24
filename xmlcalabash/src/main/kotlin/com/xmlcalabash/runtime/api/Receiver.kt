package com.xmlcalabash.runtime.api

import com.xmlcalabash.documents.XProcDocument

interface Receiver {
    fun output(port: String, document: XProcDocument)
}