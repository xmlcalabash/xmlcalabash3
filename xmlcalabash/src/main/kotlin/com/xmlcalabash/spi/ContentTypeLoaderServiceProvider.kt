package com.xmlcalabash.spi

import java.util.ServiceLoader

class ContentTypeLoaderServiceProvider {
    companion object {
        private val DEFAULT_PROVIDER = "com.xmlcalabash.util.NopContentTypeLoader"

        fun providers(): List<ContentTypeLoaderProvider> {
            val services = mutableListOf<ContentTypeLoaderProvider>()
            val loader = ServiceLoader.load(ContentTypeLoaderProvider::class.java)
            for (provider in loader.iterator()) {
                services.add(provider)
            }
            return services
        }

        fun provider(): ContentTypeLoaderProvider {
            return provider(DEFAULT_PROVIDER)
        }

        fun provider(providerName: String): ContentTypeLoaderProvider {
            val loader = ServiceLoader.load(ContentTypeLoaderProvider::class.java)
            for (provider in loader.iterator()) {
                if (provider::class.java.name == providerName) {
                    return provider
                }
            }
            throw IllegalArgumentException("Provider ${providerName} not found")
        }
    }
}