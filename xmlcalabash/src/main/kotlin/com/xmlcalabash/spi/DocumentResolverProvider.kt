package com.xmlcalabash.spi

interface DocumentResolverProvider {
    fun create(): DocumentResolver
}