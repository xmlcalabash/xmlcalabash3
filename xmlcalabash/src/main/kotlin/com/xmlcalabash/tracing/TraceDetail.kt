package com.xmlcalabash.tracing

abstract class TraceDetail(val threadId: Long) {
    val nanoSeconds = System.nanoTime()
}