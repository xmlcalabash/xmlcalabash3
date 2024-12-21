package com.xmlcalabash.tracing

import com.xmlcalabash.runtime.steps.AbstractStep

class StepStopDetail(step: AbstractStep, threadId: Long, val duration: Long): TraceDetail(threadId) {
    val id = step.id
}