package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument

interface Consumer {
    fun input(port: String, doc: XProcDocument)
    fun close(port: String)
}