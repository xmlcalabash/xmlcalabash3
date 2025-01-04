package com.xmlcalabash.test

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsErr
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MediaTypeTest {
    @Test
    fun parseTest1() {
        val typeList = MediaType.parseList("text/xml  -image/jpeg")
        if (typeList.size == 2) {
            assertEquals("text", typeList[0].mediaType)
            assertEquals("xml", typeList[0].mediaSubtype)
            assertTrue(typeList[0].inclusive)

            assertEquals("image", typeList[1].mediaType)
            assertEquals("jpeg", typeList[1].mediaSubtype)
            assertFalse(typeList[1].inclusive)
        } else {
            fail()
        }
    }

    @Test
    fun matchTest1() {
        assertTrue(MediaType.XML.matches(MediaType.ANY))
    }

    @Test
    fun matchTest2() {
        val tplain = MediaType.parse("text/plain")
        val anyXml = MediaType.parseMatch("*/*+xml")
        assertFalse(tplain.matches(anyXml))
    }
    // application/xml should match */*
    // but text/plain shouldn't match */*+xml

    @Test
    fun parseInvalidContentType() {
        try {
            MediaType.parse("something: not a content type")
            fail()
        } catch (ex: XProcException) {
            assertEquals(NsErr.xd(79), ex.error.code)
        } catch (ex: Exception) {
            fail()
        }
    }


}