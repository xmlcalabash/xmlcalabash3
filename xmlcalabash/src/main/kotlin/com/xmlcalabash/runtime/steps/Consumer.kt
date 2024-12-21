package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument

interface Consumer {
    val id: String
    fun input(port: String, doc: XProcDocument)
    fun close(port: String)
}