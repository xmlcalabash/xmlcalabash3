package com.xmlcalabash.spi

import java.util.ServiceLoader

class PagedMediaServiceProvider {
    companion object {
        private val DEFAULT_PROVIDER = "com.xmlcalabash.util.spi.StandardPagedMediaProvider"

        fun providers(): List<PagedMediaProvider> {
            val services = mutableListOf<PagedMediaProvider>()
            val loader = ServiceLoader.load(PagedMediaProvider::class.java)
            for (provider in loader.iterator()) {
                services.add(provider)
            }
            return services
        }

        fun provider(): PagedMediaProvider {
            return provider(DEFAULT_PROVIDER)
        }

        fun provider(providerName: String): PagedMediaProvider {
            val loader = ServiceLoader.load(PagedMediaProvider::class.java)
            for (provider in loader.iterator()) {
                if (provider::class.java.name == providerName) {
                    return provider
                }
            }
            throw IllegalArgumentException("Provider ${providerName} not found")
        }
    }
}