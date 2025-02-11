package com.xmlcalabash.spi

interface ContentTypeConverterProvider {
    fun create(): ContentTypeConverter
}