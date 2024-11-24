package com.xmlcalabash.test

import com.xmlcalabash.ext.uniqueid.ULID
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class UlidTest {
    @Test
    fun testUlid() {
        val id1 = ULID.next()
        val id2 = ULID.next()
        assertNotEquals(id1, id2)
    }
}