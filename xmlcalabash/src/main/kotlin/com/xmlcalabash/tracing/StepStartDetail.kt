package com.xmlcalabash.tracing

import com.xmlcalabash.runtime.steps.AbstractStep

class StepStartDetail(step: AbstractStep, threadId: Long, val startTime: Long): TraceDetail(threadId) {
    val name = step.name
    val id = step.id
    val type = step.type
}