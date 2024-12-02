package com.xmlcalabash.parsers.xpl.elements

data class StepImplementation(var resolved: Boolean, val isImplemented: () -> Boolean)