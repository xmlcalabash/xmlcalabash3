package com.xmlcalabash.spi

import java.util.*

class ConfigurerServiceProvider {
    companion object {
        private val DEFAULT_PROVIDER = "com.xmlcalabash.util.spi.StandardConfigurer"

        fun providers(): List<ConfigurerProvider> {
            val services = mutableListOf<ConfigurerProvider>()
            val loader = ServiceLoader.load(ConfigurerProvider::class.java)
            for (provider in loader.iterator()) {
                services.add(provider)
            }
            return services
        }

        fun provider(): ConfigurerProvider {
            return provider(DEFAULT_PROVIDER)
        }

        fun provider(providerName: String): ConfigurerProvider {
            val loader = ServiceLoader.load(ConfigurerProvider::class.java)
            for (provider in loader.iterator()) {
                if (provider::class.java.name == providerName) {
                    return provider
                }
            }
            throw IllegalArgumentException("Provider ${providerName} not found")
        }
    }
}