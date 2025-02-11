package com.xmlcalabash.spi

import java.util.ServiceLoader

class ContentTypeConverterServiceProvider {
    companion object {
        private val DEFAULT_PROVIDER = "com.xmlcalabash.util.NopContentTypeConverter"

        fun providers(): List<ContentTypeConverterProvider> {
            val services = mutableListOf<ContentTypeConverterProvider>()
            val loader = ServiceLoader.load(ContentTypeConverterProvider::class.java)
            for (provider in loader.iterator()) {
                services.add(provider)
            }
            return services
        }

        fun provider(): ContentTypeConverterProvider {
            return provider(DEFAULT_PROVIDER)
        }

        fun provider(providerName: String): ContentTypeConverterProvider {
            val loader = ServiceLoader.load(ContentTypeConverterProvider::class.java)
            for (provider in loader.iterator()) {
                if (provider::class.java.name == providerName) {
                    return provider
                }
            }
            throw IllegalArgumentException("Provider ${providerName} not found")
        }
    }
}