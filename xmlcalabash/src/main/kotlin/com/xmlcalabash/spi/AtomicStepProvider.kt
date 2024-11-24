package com.xmlcalabash.spi

import com.xmlcalabash.config.SaxonConfiguration

interface AtomicStepProvider {
    fun create(): AtomicStepManager
}