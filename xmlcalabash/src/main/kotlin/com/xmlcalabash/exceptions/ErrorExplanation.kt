package com.xmlcalabash.exceptions

import com.xmlcalabash.config.CommonEnvironment

interface ErrorExplanation {
    fun setEnvironment(environment: CommonEnvironment)
    fun message(error: XProcError)
    fun explanation(error: XProcError)
}