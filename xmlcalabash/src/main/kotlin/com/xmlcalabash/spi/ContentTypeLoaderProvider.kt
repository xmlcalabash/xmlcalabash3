package com.xmlcalabash.spi

interface ContentTypeLoaderProvider {
    fun create(): ContentTypeLoader
}