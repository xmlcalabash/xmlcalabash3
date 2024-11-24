package com.xmlcalabash.spi

interface PagedMediaProvider {
    fun create(): PagedMediaManager
}