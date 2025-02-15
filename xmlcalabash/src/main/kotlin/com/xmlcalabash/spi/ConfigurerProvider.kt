package com.xmlcalabash.spi

interface ConfigurerProvider {
    fun create(): Configurer
}