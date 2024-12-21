package com.xmlcalabash.tracing

import com.xmlcalabash.documents.XProcDocument

class DocumentDetail(threadId: Long, val from: Pair<String,String>, val to: Pair<String,String>, document: XProcDocument): TraceDetail(threadId) {
    val id = document.id
    val contentType = document.contentType
}