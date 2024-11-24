package com.xmlcalabash.exceptions

interface ErrorExplanation {
    fun message(error: XProcError)
    fun explanation(error: XProcError)
}