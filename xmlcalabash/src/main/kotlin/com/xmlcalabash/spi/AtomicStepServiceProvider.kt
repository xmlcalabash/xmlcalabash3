package com.xmlcalabash.spi

import java.util.ServiceLoader

class AtomicStepServiceProvider {
    companion object {
        private val DEFAULT_PROVIDER = "com.xmlcalabash.spi.StandardStepProvider"

        fun providers(): List<AtomicStepProvider> {
            val services = mutableListOf<AtomicStepProvider>()
            val loader = ServiceLoader.load(AtomicStepProvider::class.java)
            for (provider in loader.iterator()) {
                services.add(provider)
            }
            return services
        }

        fun provider(): AtomicStepProvider {
            return provider(DEFAULT_PROVIDER)
        }

        fun provider(providerName: String): AtomicStepProvider {
            val loader = ServiceLoader.load(AtomicStepProvider::class.java)
            for (provider in loader.iterator()) {
                if (provider::class.java.name == providerName) {
                    return provider
                }
            }
            throw IllegalArgumentException("Provider ${providerName} not found")
        }
    }
}