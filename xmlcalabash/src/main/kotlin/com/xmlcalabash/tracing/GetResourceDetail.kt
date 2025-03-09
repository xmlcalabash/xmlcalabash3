package com.xmlcalabash.tracing

import java.net.URI

class GetResourceDetail(val startTime: Long, val duration: Long, val uri: URI, val href: URI?, val resolved: Boolean, val cached: Boolean): TraceDetail(Thread.currentThread().id) {
}