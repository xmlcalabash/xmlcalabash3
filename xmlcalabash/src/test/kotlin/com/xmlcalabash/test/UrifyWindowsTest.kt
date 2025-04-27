package com.xmlcalabash.test

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.Urify
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.fail

class UrifyWindowsTest {
    companion object {
        private val OSNAME = "Windows"
        private val FILESEP = "\\"
        private val CWD = "C:\\Users\\JohnDoe\\"

        private var saveOsName = ""
        private var saveFilesep = ""
        private var saveCwd = ""

        @JvmStatic
        @BeforeAll
        fun setup(): Unit {
            saveOsName = Urify.osname
            saveFilesep = Urify.filesep
            saveCwd = Urify.cwd
            Urify.mockOs(OSNAME, FILESEP, CWD)
        }

        @JvmStatic
        @AfterAll
        fun teardown(): Unit {
            Urify.mockOs(saveOsName, saveFilesep, saveCwd)
        }
    }

    @Test
    fun urify_emptyString() {
        val path = Urify("")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("", path.path)
    }

    @Test
    fun urify_slash() {
        val path = Urify("/")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/", path.path)
    }

    @Test
    fun urify_slashslash() {
        val path = Urify("//")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/", path.path)
    }

    @Test
    fun urify_upassftp() {
        try {
            Urify.urify("u:pass@ftp.acme.com/dir", "ftp://ftp.example.com/")
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    fun urify_pathtothing_againstfile() {
        val path = Urify.urify("\\path\\to\\thing", "file:///root/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    fun urify_pathtothing_againsthttp() {
        val path = Urify.urify("\\path\\to\\thing", "http://example.com/")
        Assertions.assertEquals("http://example.com/path/to/thing", path)
    }

    @Test
    fun urify_001() {
        val path = Urify("///path/to/thing")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/path/to/thing", path.path)
    }

    @Test
    @DisplayName("//authority should parse")
    fun urify_002() {
        val path = Urify("//authority")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNotNull(path.authority)
        Assertions.assertEquals("authority", path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("", path.path)
    }

    @Test
    @DisplayName("//authority/ should parse")
    fun urify_003() {
        val path = Urify("//authority/")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNotNull(path.authority)
        Assertions.assertEquals("authority", path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/", path.path)
    }

    @Test
    @DisplayName("//authority/path/to/thing should parse")
    fun urify_004() {
        val path = Urify("//authority/path/to/thing")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNotNull(path.authority)
        Assertions.assertEquals("authority", path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/path/to/thing", path.path)
    }

    @Test
    @DisplayName("/Documents and Files/thing should parse")
    fun urify_005() {
        val path = Urify("/Documents and Files/thing")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/Documents and Files/thing", path.path)
    }

    @Test
    @DisplayName("/path/to/thing should parse")
    fun urify_006() {
        val path = Urify("/path/to/thing")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/path/to/thing", path.path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should parse")
    fun urify_007() {
        val path = Urify("C:/Users/Jane/Documents and Files/Thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNotNull(path.driveLetter)
        Assertions.assertEquals("C", path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/Users/Jane/Documents and Files/Thing", path.path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should parse")
    fun urify_008() {
        val path = Urify("C:Users/Jane/Documents and Files/Thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNotNull(path.driveLetter)
        Assertions.assertEquals("C", path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("Users/Jane/Documents and Files/Thing", path.path)
    }

    @Test
    @DisplayName("Documents and Files/thing should parse")
    fun urify_009() {
        val path = Urify("Documents and Files/thing")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("Documents and Files/thing", path.path)
    }

    @Test
    @DisplayName("file: should parse")
    fun urify_010() {
        val path = Urify("file:")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("", path.path)
    }

    @Test
    @DisplayName("file:///path/to/thing should parse")
    fun urify_011() {
        val path = Urify("file:///path/to/thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/path/to/thing", path.path)
    }

    @Test
    @DisplayName("file://authority.com should parse")
    fun urify_012() {
        val path = Urify("file://authority.com")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNotNull(path.authority)
        Assertions.assertEquals("authority.com", path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("", path.path)
    }

    @Test
    @DisplayName("file://authority.com/ should parse")
    fun urify_013() {
        val path = Urify("file://authority.com/")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNotNull(path.authority)
        Assertions.assertEquals("authority.com", path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/", path.path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should parse")
    fun urify_014() {
        val path = Urify("file://authority.com/path/to/thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNotNull(path.authority)
        Assertions.assertEquals("authority.com", path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/path/to/thing", path.path)
    }

    @Test
    @DisplayName("file:/path/to/thing should parse")
    fun urify_015() {
        val path = Urify("file:/path/to/thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/path/to/thing", path.path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should parse")
    fun urify_016() {
        val path = Urify("file:C:/Users/Jane/Documents and Files/Thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNotNull(path.driveLetter)
        Assertions.assertEquals("C", path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/Users/Jane/Documents and Files/Thing", path.path)
    }

    @Test
    @DisplayName("file:path/to/thing should parse")
    fun urify_018() {
        val path = Urify("file:path/to/thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("file", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("path/to/thing", path.path)
    }

    @Test
    @DisplayName("https: should parse")
    fun urify_019() {
        val path = Urify("https:")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("https", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("", path.path)
    }

    @Test
    @DisplayName("https://example.com should parse")
    fun urify_020() {
        val path = Urify("https://example.com")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("https", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("//example.com", path.path)
    }

    @Test
    @DisplayName("https://example.com/ should parse")
    fun urify_021() {
        val path = Urify("https://example.com/")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("https", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("//example.com/", path.path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should parse")
    fun urify_022() {
        val path = Urify("https://example.com/path/to/thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("https", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("//example.com/path/to/thing", path.path)
    }

    @Test
    @DisplayName("path/to/thing should parse")
    fun urify_023() {
        val path = Urify("path/to/thing")
        Assertions.assertNull(path.scheme)
        Assertions.assertFalse(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("path/to/thing", path.path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should parse")
    fun urify_024() {
        val path = Urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("urn", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertFalse(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path.path)
    }

    @Test
    @DisplayName("///path/to/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_025() {
        val path = Urify.urify("///path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/path/to/thing", path)
    }

    @Test
    @DisplayName("//authority should throw an exception against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_026() {
        try {
            Urify.urify("//authority", "file:///C:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0076", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority/ should throw an exception against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_027() {
        try {
            Urify.urify("//authority/", "file:///C:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0076", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority/path/to/thing should throw an exception against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_028() {
        try {
            Urify.urify("//authority/path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0076", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("/Documents and Files/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_029() {
        val path = Urify.urify("/Documents and Files/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("/path/to/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_030() {
        val path = Urify.urify("/path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/path/to/thing", path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_031() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents%20and%20Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_032() {
        val path = Urify.urify("C:Users/Jane/Documents and Files/Thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane%20Doe/Documents/Users/Jane/Documents%20and%20Files/Thing", path)
    }

    @Test
    @DisplayName("Documents and Files/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_033() {
        val path = Urify.urify("Documents and Files/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane%20Doe/Documents/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("file: should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_034() {
        val path = Urify.urify("file:", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane%20Doe/Documents/", path)
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_035() {
        val path = Urify.urify("file:///path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should throw an exception against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_036() {
        try {
            Urify.urify("file://authority.com", "file:///C:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0076", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_037() {
        val path = Urify.urify("file://authority.com/", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_038() {
        val path = Urify.urify("file://authority.com/path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_039() {
        val path = Urify.urify("file:/path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_040() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_041() {
        val path = Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane%20Doe/Documents/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:path/to/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_042() {
        val path = Urify.urify("file:path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane%20Doe/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("https: should throw an exception against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_043() {
        try {
            Urify.urify("https:", "file:///C:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_044() {
        val path = Urify.urify("https://example.com", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_045() {
        val path = Urify.urify("https://example.com/", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_046() {
        val path = Urify.urify("https://example.com/path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_047() {
        val path = Urify.urify("path/to/thing", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane%20Doe/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against file:///C:/Users/Jane%20Doe/Documents/")
    fun urify_048() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file:///C:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_049() {
        val path = Urify.urify("///path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///D:/path/to/thing", path)
    }

    @Test
    @DisplayName("//authority should throw an exception against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_050() {
        try {
            Urify.urify("//authority", "file:///D:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0076", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority/ should throw an exception against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_051() {
        try {
            Urify.urify("//authority/", "file:///D:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0076", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority/path/to/thing should throw an exception against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_052() {
        try {
            Urify.urify("//authority/path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0076", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("/Documents and Files/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_053() {
        val path = Urify.urify("/Documents and Files/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///D:/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("/path/to/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_054() {
        val path = Urify.urify("/path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///D:/path/to/thing", path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_055() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents%20and%20Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_056() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "file:///D:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_057() {
        val path = Urify.urify("Documents and Files/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///D:/Users/Jane%20Doe/Documents/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("file: should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_058() {
        val path = Urify.urify("file:", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///D:/Users/Jane%20Doe/Documents/", path)
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_059() {
        val path = Urify.urify("file:///path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should throw an exception against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_060() {
        try {
            Urify.urify("file://authority.com", "file:///D:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0076", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_061() {
        val path = Urify.urify("file://authority.com/", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_062() {
        val path = Urify.urify("file://authority.com/path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_063() {
        val path = Urify.urify("file:/path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_064() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_065() {
        try {
            Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file:///D:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:path/to/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_066() {
        val path = Urify.urify("file:path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///D:/Users/Jane%20Doe/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("https: should throw an exception against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_067() {
        try {
            Urify.urify("https:", "file:///D:/Users/Jane%20Doe/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_068() {
        val path = Urify.urify("https://example.com", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_069() {
        val path = Urify.urify("https://example.com/", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_070() {
        val path = Urify.urify("https://example.com/path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_071() {
        val path = Urify.urify("path/to/thing", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("file:///D:/Users/Jane%20Doe/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against file:///D:/Users/Jane%20Doe/Documents/")
    fun urify_072() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file:///D:/Users/Jane%20Doe/Documents/")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should resolve against file://hostname/Documents/")
    fun urify_073() {
        val path = Urify.urify("///path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/path/to/thing", path)
    }

    @Test
    @DisplayName("//authority should resolve against file://hostname/Documents/")
    fun urify_074() {
        val path = Urify.urify("//authority", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority", path)
    }

    @Test
    @DisplayName("//authority/ should resolve against file://hostname/Documents/")
    fun urify_075() {
        val path = Urify.urify("//authority/", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority/", path)
    }

    @Test
    @DisplayName("//authority/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_076() {
        val path = Urify.urify("//authority/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority/path/to/thing", path)
    }

    @Test
    @DisplayName("/Documents and Files/thing should resolve against file://hostname/Documents/")
    fun urify_077() {
        val path = Urify.urify("/Documents and Files/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_078() {
        val path = Urify.urify("/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/path/to/thing", path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against file://hostname/Documents/")
    fun urify_079() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents%20and%20Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against file://hostname/Documents/")
    fun urify_080() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should resolve against file://hostname/Documents/")
    fun urify_081() {
        val path = Urify.urify("Documents and Files/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("file: should resolve against file://hostname/Documents/")
    fun urify_082() {
        val path = Urify.urify("file:", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/", path)
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against file://hostname/Documents/")
    fun urify_083() {
        val path = Urify.urify("file:///path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should resolve against file://hostname/Documents/")
    fun urify_084() {
        val path = Urify.urify("file://authority.com", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority.com", path)
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against file://hostname/Documents/")
    fun urify_085() {
        val path = Urify.urify("file://authority.com/", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_086() {
        val path = Urify.urify("file://authority.com/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_087() {
        val path = Urify.urify("file:/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against file://hostname/Documents/")
    fun urify_088() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against file://hostname/Documents/")
    fun urify_089() {
        try {
            Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:path/to/thing should resolve against file://hostname/Documents/")
    fun urify_090() {
        val path = Urify.urify("file:path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("https: should throw an exception against file://hostname/Documents/")
    fun urify_091() {
        try {
            Urify.urify("https:", "file://hostname/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against file://hostname/Documents/")
    fun urify_092() {
        val path = Urify.urify("https://example.com", "file://hostname/Documents/")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against file://hostname/Documents/")
    fun urify_093() {
        val path = Urify.urify("https://example.com/", "file://hostname/Documents/")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_094() {
        val path = Urify.urify("https://example.com/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should resolve against file://hostname/Documents/")
    fun urify_095() {
        val path = Urify.urify("path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against file://hostname/Documents/")
    fun urify_096() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file://hostname/Documents/")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should resolve against http://example.com/Documents/")
    fun urify_097() {
        val path = Urify.urify("///path/to/thing", "http://example.com/Documents/")
        Assertions.assertEquals("http://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("//authority should resolve against http://example.com/Documents/")
    fun urify_098() {
        val path = Urify.urify("//authority", "http://example.com/Documents/")
        Assertions.assertEquals("http://authority", path)
    }

    @Test
    @DisplayName("//authority/ should resolve against http://example.com/Documents/")
    fun urify_099() {
        val path = Urify.urify("//authority/", "http://example.com/Documents/")
        Assertions.assertEquals("http://authority/", path)
    }

    @Test
    @DisplayName("//authority/path/to/thing should resolve against http://example.com/Documents/")
    fun urify_100() {
        val path = Urify.urify("//authority/path/to/thing", "http://example.com/Documents/")
        Assertions.assertEquals("http://authority/path/to/thing", path)
    }

    @Test
    @DisplayName("/Documents and Files/thing should resolve against http://example.com/Documents/")
    fun urify_101() {
        val path = Urify.urify("/Documents and Files/thing", "http://example.com/Documents/")
        Assertions.assertEquals("http://example.com/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("/path/to/thing should resolve against http://example.com/Documents/")
    fun urify_102() {
        val path = Urify.urify("/path/to/thing", "http://example.com/Documents/")
        Assertions.assertEquals("http://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against http://example.com/Documents/")
    fun urify_103() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "http://example.com/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents%20and%20Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against http://example.com/Documents/")
    fun urify_104() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "http://example.com/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should resolve against http://example.com/Documents/")
    fun urify_105() {
        val path = Urify.urify("Documents and Files/thing", "http://example.com/Documents/")
        Assertions.assertEquals("http://example.com/Documents/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("file: should throw an exception against http://example.com/Documents/")
    fun urify_106() {
        try {
            Urify.urify("file:", "http://example.com/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against http://example.com/Documents/")
    fun urify_107() {
        val path = Urify.urify("file:///path/to/thing", "http://example.com/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should throw an exception against http://example.com/Documents/")
    fun urify_108() {
        try {
            Urify.urify("file://authority.com", "http://example.com/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against http://example.com/Documents/")
    fun urify_109() {
        val path = Urify.urify("file://authority.com/", "http://example.com/Documents/")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against http://example.com/Documents/")
    fun urify_110() {
        val path = Urify.urify("file://authority.com/path/to/thing", "http://example.com/Documents/")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against http://example.com/Documents/")
    fun urify_111() {
        val path = Urify.urify("file:/path/to/thing", "http://example.com/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against http://example.com/Documents/")
    fun urify_112() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "http://example.com/Documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against http://example.com/Documents/")
    fun urify_113() {
        try {
            Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "http://example.com/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:path/to/thing should throw an exception against http://example.com/Documents/")
    fun urify_114() {
        try {
            Urify.urify("file:path/to/thing", "http://example.com/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https: should throw an exception against http://example.com/Documents/")
    fun urify_115() {
        try {
            Urify.urify("https:", "http://example.com/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against http://example.com/Documents/")
    fun urify_116() {
        val path = Urify.urify("https://example.com", "http://example.com/Documents/")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against http://example.com/Documents/")
    fun urify_117() {
        val path = Urify.urify("https://example.com/", "http://example.com/Documents/")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against http://example.com/Documents/")
    fun urify_118() {
        val path = Urify.urify("https://example.com/path/to/thing", "http://example.com/Documents/")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should resolve against http://example.com/Documents/")
    fun urify_119() {
        val path = Urify.urify("path/to/thing", "http://example.com/Documents/")
        Assertions.assertEquals("http://example.com/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against http://example.com/Documents/")
    fun urify_120() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "http://example.com/Documents/")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_121() {
        val path = Urify.urify("///path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("//authority should resolve against file:///home/jdoe/documents/")
    fun urify_122() {
        val path = Urify.urify("//authority", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority", path)
    }

    @Test
    @DisplayName("//authority/ should resolve against file:///home/jdoe/documents/")
    fun urify_123() {
        val path = Urify.urify("//authority/", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority/", path)
    }

    @Test
    @DisplayName("//authority/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_124() {
        val path = Urify.urify("//authority/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority/path/to/thing", path)
    }

    @Test
    @DisplayName("/Documents and Files/thing should resolve against file:///home/jdoe/documents/")
    fun urify_125() {
        val path = Urify.urify("/Documents and Files/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_126() {
        val path = Urify.urify("/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against file:///home/jdoe/documents/")
    fun urify_127() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents%20and%20Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against file:///home/jdoe/documents/")
    fun urify_128() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should resolve against file:///home/jdoe/documents/")
    fun urify_129() {
        val path = Urify.urify("Documents and Files/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("file: should resolve against file:///home/jdoe/documents/")
    fun urify_130() {
        val path = Urify.urify("file:", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/", path)
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_131() {
        val path = Urify.urify("file:///path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should resolve against file:///home/jdoe/documents/")
    fun urify_132() {
        val path = Urify.urify("file://authority.com", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority.com", path)
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against file:///home/jdoe/documents/")
    fun urify_133() {
        val path = Urify.urify("file://authority.com/", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_134() {
        val path = Urify.urify("file://authority.com/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_135() {
        val path = Urify.urify("file:/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against file:///home/jdoe/documents/")
    fun urify_136() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against file:///home/jdoe/documents/")
    fun urify_137() {
        try {
            Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0075", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_138() {
        val path = Urify.urify("file:path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/path/to/thing", path)
    }

    @Test
    @DisplayName("https: should throw an exception against file:///home/jdoe/documents/")
    fun urify_139() {
        try {
            Urify.urify("https:", "file:///home/jdoe/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against file:///home/jdoe/documents/")
    fun urify_140() {
        val path = Urify.urify("https://example.com", "file:///home/jdoe/documents/")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against file:///home/jdoe/documents/")
    fun urify_141() {
        val path = Urify.urify("https://example.com/", "file:///home/jdoe/documents/")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_142() {
        val path = Urify.urify("https://example.com/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_143() {
        val path = Urify.urify("path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/path/to/thing", path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against file:///home/jdoe/documents/")
    fun urify_144() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file:///home/jdoe/documents/")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_145() {
        try {
            Urify.urify("///path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_146() {
        try {
            Urify.urify("//authority", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority/ should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_147() {
        try {
            Urify.urify("//authority/", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority/path/to/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_148() {
        try {
            Urify.urify("//authority/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("/Documents and Files/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_149() {
        try {
            Urify.urify("/Documents and Files/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("/path/to/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_150() {
        try {
            Urify.urify("/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_151() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents%20and%20Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_152() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_153() {
        try {
            Urify.urify("Documents and Files/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file: should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_154() {
        try {
            Urify.urify("file:", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_155() {
        val path = Urify.urify("file:///path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_156() {
        try {
            Urify.urify("file://authority.com", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_157() {
        val path = Urify.urify("file://authority.com/", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_158() {
        val path = Urify.urify("file://authority.com/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_159() {
        val path = Urify.urify("file:/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_160() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_161() {
        try {
            Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:path/to/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_162() {
        try {
            Urify.urify("file:path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https: should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_163() {
        try {
            Urify.urify("https:", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_164() {
        val path = Urify.urify("https://example.com", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_165() {
        val path = Urify.urify("https://example.com/", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_166() {
        val path = Urify.urify("https://example.com/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_167() {
        try {
            Urify.urify("path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_168() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should throw an exception against file:not-absolute")
    fun urify_169() {
        try {
            Urify.urify("///path/to/thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority should throw an exception against file:not-absolute")
    fun urify_170() {
        try {
            Urify.urify("//authority", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority/ should throw an exception against file:not-absolute")
    fun urify_171() {
        try {
            Urify.urify("//authority/", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("//authority/path/to/thing should throw an exception against file:not-absolute")
    fun urify_172() {
        try {
            Urify.urify("//authority/path/to/thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("/Documents and Files/thing should throw an exception against file:not-absolute")
    fun urify_173() {
        try {
            Urify.urify("/Documents and Files/thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("/path/to/thing should throw an exception against file:not-absolute")
    fun urify_174() {
        try {
            Urify.urify("/path/to/thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against file:not-absolute")
    fun urify_175() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file:not-absolute")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents%20and%20Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against file:not-absolute")
    fun urify_176() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should throw an exception against file:not-absolute")
    fun urify_177() {
        try {
            Urify.urify("Documents and Files/thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file: should throw an exception against file:not-absolute")
    fun urify_178() {
        try {
            Urify.urify("file:", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against file:not-absolute")
    fun urify_179() {
        val path = Urify.urify("file:///path/to/thing", "file:not-absolute")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should throw an exception against file:not-absolute")
    fun urify_180() {
        try {
            Urify.urify("file://authority.com", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against file:not-absolute")
    fun urify_181() {
        val path = Urify.urify("file://authority.com/", "file:not-absolute")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against file:not-absolute")
    fun urify_182() {
        val path = Urify.urify("file://authority.com/path/to/thing", "file:not-absolute")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against file:not-absolute")
    fun urify_183() {
        val path = Urify.urify("file:/path/to/thing", "file:not-absolute")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against file:not-absolute")
    fun urify_184() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file:not-absolute")
        Assertions.assertEquals("file:///C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against file:not-absolute")
    fun urify_185() {
        try {
            Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:path/to/thing should throw an exception against file:not-absolute")
    fun urify_186() {
        try {
            Urify.urify("file:path/to/thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https: should throw an exception against file:not-absolute")
    fun urify_187() {
        try {
            Urify.urify("https:", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against file:not-absolute")
    fun urify_188() {
        val path = Urify.urify("https://example.com", "file:not-absolute")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against file:not-absolute")
    fun urify_189() {
        val path = Urify.urify("https://example.com/", "file:not-absolute")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against file:not-absolute")
    fun urify_190() {
        val path = Urify.urify("https://example.com/path/to/thing", "file:not-absolute")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should throw an exception against file:not-absolute")
    fun urify_192() {
        try {
            Urify.urify("path/to/thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against file:not-absolute")
    fun urify_193() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file:not-absolute")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

}