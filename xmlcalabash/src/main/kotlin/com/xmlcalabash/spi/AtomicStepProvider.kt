package com.xmlcalabash.spi

interface AtomicStepProvider {
    fun create(): AtomicStepManager
}