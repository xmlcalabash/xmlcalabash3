package com.xmlcalabash.exceptions

import com.xmlcalabash.config.CommonEnvironment

interface ErrorExplanation {
    fun setEnvironment(environment: CommonEnvironment)
    fun report(error: XProcError)
    fun reportExplanation(error: XProcError)
    fun message(error: XProcError, includeCause: Boolean): String
    fun explanation(error: XProcError): String
}