package com.xmlcalabash.exceptions

interface ErrorExplanation {
    fun report(error: XProcError)
    fun reportExplanation(error: XProcError)
    fun message(error: XProcError, includeDetails: Boolean): String
    fun explanation(error: XProcError): String
}