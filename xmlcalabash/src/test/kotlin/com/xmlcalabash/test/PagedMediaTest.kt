package com.xmlcalabash.test

import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaServiceProvider
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PagedMediaTest {
    @Test
    fun nopXslFormatter() {
        val providers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            providers.add(provider.create())
        }
        Assertions.assertTrue(providers.isNotEmpty())
    }
}