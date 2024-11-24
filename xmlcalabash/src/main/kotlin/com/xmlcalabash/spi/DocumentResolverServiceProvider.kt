package com.xmlcalabash.spi

import java.util.ServiceLoader

class DocumentResolverServiceProvider {
    companion object {
        private val DEFAULT_PROVIDER = "com.xmlcalabash.spi.NopDocumentManagerProvider"

        fun providers(): List<DocumentResolverProvider> {
            val services = mutableListOf<DocumentResolverProvider>()
            val loader = ServiceLoader.load(DocumentResolverProvider::class.java)
            for (provider in loader.iterator()) {
                services.add(provider)
            }
            return services
        }

        fun provider(): DocumentResolverProvider {
            return provider(DEFAULT_PROVIDER)
        }

        fun provider(providerName: String): DocumentResolverProvider {
            val loader = ServiceLoader.load(DocumentResolverProvider::class.java)
            for (provider in loader.iterator()) {
                if (provider::class.java.name == providerName) {
                    return provider
                }
            }
            throw IllegalArgumentException("Provider ${providerName} not found")
        }
    }
}