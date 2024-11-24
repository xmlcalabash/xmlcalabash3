package com.xmlcalabash.test

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.Urify
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.fail

class UrifyNonWindowsTest {
    companion object {
        private val OSNAME = "MacOS"
        private val FILESEP = "/"
        private val CWD = "/home/johndoe"

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
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    fun urify_pathtothing_againstfile() {
        val path = Urify.urify("\\path\\to\\thing", "file:///root/")
        Assertions.assertEquals("file:///root/%5Cpath%5Cto%5Cthing", path)
    }

    @Test
    fun urify_pathtothing_againsthttp() {
        val path = Urify.urify("\\path\\to\\thing", "http://example.com/")
        Assertions.assertEquals("http://example.com/%5Cpath%5Cto%5Cthing", path)
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
        Assertions.assertEquals("C", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
        Assertions.assertTrue(path.absolute)
        Assertions.assertFalse(path.relative)
        Assertions.assertEquals("/Users/Jane/Documents and Files/Thing", path.path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should parse")
    fun urify_008() {
        val path = Urify("C:Users/Jane/Documents and Files/Thing")
        Assertions.assertNotNull(path.scheme)
        Assertions.assertEquals("C", path.scheme)
        Assertions.assertTrue(path.explicit)
        Assertions.assertTrue(path.hierarchical)
        Assertions.assertNull(path.authority)
        Assertions.assertNull(path.driveLetter)
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
        Assertions.assertNull(path.driveLetter)
        Assertions.assertFalse(path.absolute)
        Assertions.assertTrue(path.relative)
        Assertions.assertEquals("C:/Users/Jane/Documents and Files/Thing", path.path)
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
    @DisplayName("///path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_025() {
        val path = Urify.urify("///path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("//authority should resolve against file:///home/jdoe/documents/")
    fun urify_026() {
        val path = Urify.urify("//authority", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority", path)
    }

    @Test
    @DisplayName("//authority/ should resolve against file:///home/jdoe/documents/")
    fun urify_027() {
        val path = Urify.urify("//authority/", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority/", path)
    }

    @Test
    @DisplayName("//authority/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_028() {
        val path = Urify.urify("//authority/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority/path/to/thing", path)
    }

    @Test
    @DisplayName("/Documents and Files/thing should resolve against file:///home/jdoe/documents/")
    fun urify_029() {
        val path = Urify.urify("/Documents and Files/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_030() {
        val path = Urify.urify("/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against file:///home/jdoe/documents/")
    fun urify_031() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against file:///home/jdoe/documents/")
    fun urify_032() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should resolve against file:///home/jdoe/documents/")
    fun urify_033() {
        val path = Urify.urify("Documents and Files/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("file: should resolve against file:///home/jdoe/documents/")
    fun urify_034() {
        val path = Urify.urify("file:", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/", path)
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_035() {
        val path = Urify.urify("file:///path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should resolve against file:///home/jdoe/documents/")
    fun urify_036() {
        val path = Urify.urify("file://authority.com", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority.com", path)
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against file:///home/jdoe/documents/")
    fun urify_037() {
        val path = Urify.urify("file://authority.com/", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_038() {
        val path = Urify.urify("file://authority.com/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_039() {
        val path = Urify.urify("file:/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against file:///home/jdoe/documents/")
    fun urify_040() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should resolve against file:///home/jdoe/documents/")
    fun urify_041() {
        val path = Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/C:Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_042() {
        val path = Urify.urify("file:path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/path/to/thing", path)
    }

    @Test
    @DisplayName("https: should throw an exception against file:///home/jdoe/documents/")
    fun urify_043() {
        try {
            Urify.urify("https:", "file:///home/jdoe/documents/")
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against file:///home/jdoe/documents/")
    fun urify_044() {
        val path = Urify.urify("https://example.com", "file:///home/jdoe/documents/")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against file:///home/jdoe/documents/")
    fun urify_045() {
        val path = Urify.urify("https://example.com/", "file:///home/jdoe/documents/")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_046() {
        val path = Urify.urify("https://example.com/path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should resolve against file:///home/jdoe/documents/")
    fun urify_047() {
        val path = Urify.urify("path/to/thing", "file:///home/jdoe/documents/")
        Assertions.assertEquals("file:///home/jdoe/documents/path/to/thing", path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against file:///home/jdoe/documents/")
    fun urify_048() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file:///home/jdoe/documents/")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should resolve against http://example.com/documents/")
    fun urify_049() {
        val path = Urify.urify("///path/to/thing", "http://example.com/documents/")
        Assertions.assertEquals("http://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("//authority should resolve against http://example.com/documents/")
    fun urify_050() {
        val path = Urify.urify("//authority", "http://example.com/documents/")
        Assertions.assertEquals("http://authority", path)
    }

    @Test
    @DisplayName("//authority/ should resolve against http://example.com/documents/")
    fun urify_051() {
        val path = Urify.urify("//authority/", "http://example.com/documents/")
        Assertions.assertEquals("http://authority/", path)
    }

    @Test
    @DisplayName("//authority/path/to/thing should resolve against http://example.com/documents/")
    fun urify_052() {
        val path = Urify.urify("//authority/path/to/thing", "http://example.com/documents/")
        Assertions.assertEquals("http://authority/path/to/thing", path)
    }

    @Test
    @DisplayName("/Documents and Files/thing should resolve against http://example.com/documents/")
    fun urify_053() {
        val path = Urify.urify("/Documents and Files/thing", "http://example.com/documents/")
        Assertions.assertEquals("http://example.com/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("/path/to/thing should resolve against http://example.com/documents/")
    fun urify_054() {
        val path = Urify.urify("/path/to/thing", "http://example.com/documents/")
        Assertions.assertEquals("http://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against http://example.com/documents/")
    fun urify_055() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "http://example.com/documents/")
        Assertions.assertEquals("C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against http://example.com/documents/")
    fun urify_056() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "http://example.com/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should resolve against http://example.com/documents/")
    fun urify_057() {
        val path = Urify.urify("Documents and Files/thing", "http://example.com/documents/")
        Assertions.assertEquals("http://example.com/documents/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("file: should throw an exception against http://example.com/documents/")
    fun urify_058() {
        try {
            Urify.urify("file:", "http://example.com/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against http://example.com/documents/")
    fun urify_059() {
        val path = Urify.urify("file:///path/to/thing", "http://example.com/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should throw an exception against http://example.com/documents/")
    fun urify_060() {
        try {
            Urify.urify("file://authority.com", "http://example.com/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against http://example.com/documents/")
    fun urify_061() {
        val path = Urify.urify("file://authority.com/", "http://example.com/documents/")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against http://example.com/documents/")
    fun urify_062() {
        val path = Urify.urify("file://authority.com/path/to/thing", "http://example.com/documents/")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against http://example.com/documents/")
    fun urify_063() {
        val path = Urify.urify("file:/path/to/thing", "http://example.com/documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should throw an exception against http://example.com/documents/")
    fun urify_064() {
        try {
            Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "http://example.com/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against http://example.com/documents/")
    fun urify_065() {
        try {
            Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "http://example.com/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:path/to/thing should throw an exception against http://example.com/documents/")
    fun urify_066() {
        try {
            Urify.urify("file:path/to/thing", "http://example.com/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https: should throw an exception against http://example.com/documents/")
    fun urify_067() {
        try {
            Urify.urify("https:", "http://example.com/documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("https://example.com should resolve against http://example.com/documents/")
    fun urify_068() {
        val path = Urify.urify("https://example.com", "http://example.com/documents/")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against http://example.com/documents/")
    fun urify_069() {
        val path = Urify.urify("https://example.com/", "http://example.com/documents/")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against http://example.com/documents/")
    fun urify_070() {
        val path = Urify.urify("https://example.com/path/to/thing", "http://example.com/documents/")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should resolve against http://example.com/documents/")
    fun urify_071() {
        val path = Urify.urify("path/to/thing", "http://example.com/documents/")
        Assertions.assertEquals("http://example.com/documents/path/to/thing", path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against http://example.com/documents/")
    fun urify_072() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "http://example.com/documents/")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_073() {
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
    fun urify_074() {
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
    fun urify_075() {
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
    fun urify_076() {
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
    fun urify_077() {
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
    fun urify_078() {
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
    fun urify_079() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_080() {
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
    fun urify_081() {
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
    fun urify_082() {
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
    fun urify_083() {
        val path = Urify.urify("file:///path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_084() {
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
    fun urify_085() {
        val path = Urify.urify("file://authority.com/", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_086() {
        val path = Urify.urify("file://authority.com/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_087() {
        val path = Urify.urify("file:/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_088() {
        try {
            Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0080", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_089() {
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
    fun urify_090() {
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
    fun urify_091() {
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
    fun urify_092() {
        val path = Urify.urify("https://example.com", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_093() {
        val path = Urify.urify("https://example.com/", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_094() {
        val path = Urify.urify("https://example.com/path/to/thing", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should throw an exception against urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
    fun urify_095() {
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
    fun urify_096() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should resolve against file://hostname/Documents/")
    fun urify_097() {
        val path = Urify.urify("///path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/path/to/thing", path)
    }

    @Test
    @DisplayName("//authority should resolve against file://hostname/Documents/")
    fun urify_098() {
        val path = Urify.urify("//authority", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority", path)
    }

    @Test
    @DisplayName("//authority/ should resolve against file://hostname/Documents/")
    fun urify_099() {
        val path = Urify.urify("//authority/", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority/", path)
    }

    @Test
    @DisplayName("//authority/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_100() {
        val path = Urify.urify("//authority/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority/path/to/thing", path)
    }

    @Test
    @DisplayName("/Documents and Files/thing should resolve against file://hostname/Documents/")
    fun urify_101() {
        val path = Urify.urify("/Documents and Files/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_102() {
        val path = Urify.urify("/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/path/to/thing", path)
    }

    @Test
    @DisplayName("C:/Users/Jane/Documents and Files/Thing should resolve against file://hostname/Documents/")
    fun urify_103() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
        Assertions.assertEquals("C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against file://hostname/Documents/")
    fun urify_104() {
        try {
            Urify.urify("C:Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0077", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("Documents and Files/thing should resolve against file://hostname/Documents/")
    fun urify_105() {
        val path = Urify.urify("Documents and Files/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/Documents%20and%20Files/thing", path)
    }

    @Test
    @DisplayName("file: should resolve against file://hostname/Documents/")
    fun urify_106() {
        val path = Urify.urify("file:", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/", path)
    }

    @Test
    @DisplayName("file:///path/to/thing should resolve against file://hostname/Documents/")
    fun urify_107() {
        val path = Urify.urify("file:///path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should resolve against file://hostname/Documents/")
    fun urify_108() {
        val path = Urify.urify("file://authority.com", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority.com", path)
    }

    @Test
    @DisplayName("file://authority.com/ should resolve against file://hostname/Documents/")
    fun urify_109() {
        val path = Urify.urify("file://authority.com/", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_110() {
        val path = Urify.urify("file://authority.com/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_111() {
        val path = Urify.urify("file:/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should resolve against file://hostname/Documents/")
    fun urify_112() {
        val path = Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should resolve against file://hostname/Documents/")
    fun urify_113() {
        val path = Urify.urify("file:C:Users/Jane/Documents and Files/Thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/C:Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("file:path/to/thing should resolve against file://hostname/Documents/")
    fun urify_114() {
        val path = Urify.urify("file:path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("https: should throw an exception against file://hostname/Documents/")
    fun urify_115() {
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
    fun urify_116() {
        val path = Urify.urify("https://example.com", "file://hostname/Documents/")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against file://hostname/Documents/")
    fun urify_117() {
        val path = Urify.urify("https://example.com/", "file://hostname/Documents/")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against file://hostname/Documents/")
    fun urify_118() {
        val path = Urify.urify("https://example.com/path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should resolve against file://hostname/Documents/")
    fun urify_119() {
        val path = Urify.urify("path/to/thing", "file://hostname/Documents/")
        Assertions.assertEquals("file://hostname/Documents/path/to/thing", path)
    }

    @Test
    @DisplayName("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN should resolve against file://hostname/Documents/")
    fun urify_120() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file://hostname/Documents/")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }

    @Test
    @DisplayName("///path/to/thing should throw an exception against file:not-absolute")
    fun urify_121() {
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
    fun urify_122() {
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
    fun urify_123() {
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
    fun urify_124() {
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
    fun urify_125() {
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
    fun urify_126() {
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
    fun urify_127() {
        val path = Urify.urify("C:/Users/Jane/Documents and Files/Thing", "file:not-absolute")
        Assertions.assertEquals("C:/Users/Jane/Documents and Files/Thing", path)
    }

    @Test
    @DisplayName("C:Users/Jane/Documents and Files/Thing should throw an exception against file:not-absolute")
    fun urify_128() {
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
    fun urify_129() {
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
    fun urify_130() {
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
    fun urify_131() {
        val path = Urify.urify("file:///path/to/thing", "file:not-absolute")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file://authority.com should throw an exception against file:not-absolute")
    fun urify_132() {
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
    fun urify_133() {
        val path = Urify.urify("file://authority.com/", "file:not-absolute")
        Assertions.assertEquals("file://authority.com/", path)
    }

    @Test
    @DisplayName("file://authority.com/path/to/thing should resolve against file:not-absolute")
    fun urify_134() {
        val path = Urify.urify("file://authority.com/path/to/thing", "file:not-absolute")
        Assertions.assertEquals("file://authority.com/path/to/thing", path)
    }

    @Test
    @DisplayName("file:/path/to/thing should resolve against file:not-absolute")
    fun urify_135() {
        val path = Urify.urify("file:/path/to/thing", "file:not-absolute")
        Assertions.assertEquals("file:///path/to/thing", path)
    }

    @Test
    @DisplayName("file:C:/Users/Jane/Documents and Files/Thing should throw an exception against file:not-absolute")
    fun urify_136() {
        try {
            Urify.urify("file:C:/Users/Jane/Documents and Files/Thing", "file:not-absolute")
            fail()
        } catch(ex: XProcException) {
            Assertions.assertEquals("XD0074", ex.error.code.localName)
        } catch (ex: Throwable) {
            fail()
        }
    }

    @Test
    @DisplayName("file:C:Users/Jane/Documents and Files/Thing should throw an exception against file:not-absolute")
    fun urify_137() {
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
    fun urify_138() {
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
    fun urify_139() {
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
    fun urify_140() {
        val path = Urify.urify("https://example.com", "file:not-absolute")
        Assertions.assertEquals("https://example.com", path)
    }

    @Test
    @DisplayName("https://example.com/ should resolve against file:not-absolute")
    fun urify_141() {
        val path = Urify.urify("https://example.com/", "file:not-absolute")
        Assertions.assertEquals("https://example.com/", path)
    }

    @Test
    @DisplayName("https://example.com/path/to/thing should resolve against file:not-absolute")
    fun urify_142() {
        val path = Urify.urify("https://example.com/path/to/thing", "file:not-absolute")
        Assertions.assertEquals("https://example.com/path/to/thing", path)
    }

    @Test
    @DisplayName("path/to/thing should throw an exception against file:not-absolute")
    fun urify_143() {
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
    fun urify_144() {
        val path = Urify.urify("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", "file:not-absolute")
        Assertions.assertEquals("urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN", path)
    }
}