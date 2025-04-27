package com.xmlcalabash.test

import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.util.Urify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI

class UriTestWindows {
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
    fun normalize_path_1() {
        val path = UriUtils.normalizePath("/path/to/thing")
        Assertions.assertEquals("/path/to/thing", path)
    }

    @Test
    fun normalize_path_2() {
        val path = UriUtils.normalizePath("path/to/thing")
        Assertions.assertEquals("path/to/thing", path)
    }

    @Test
    fun normalize_path_3() {
        val path = UriUtils.normalizePath("path/to/thing/")
        Assertions.assertEquals("path/to/thing/", path)
    }

    @Test
    fun normalize_path_4() {
        val path = UriUtils.normalizePath("path/./to/thing/.")
        Assertions.assertEquals("path/to/thing/", path)
    }

    @Test
    fun normalize_path_5() {
        val path = UriUtils.normalizePath("path/to/../thing/")
        Assertions.assertEquals("path/thing/", path)
    }

    @Test
    fun normalize_path_6() {
        val path = UriUtils.normalizePath("path/to/thing/..")
        Assertions.assertEquals("path/to/", path)
    }

    @Test
    fun normalize_path_7() {
        val path = UriUtils.normalizePath("path/to/thing/.././././path/..")
        Assertions.assertEquals("path/to/", path)
    }

    @Test
    fun normalize_path_8() {
        val path = UriUtils.normalizePath("C:\\path\\to\\thing")
        Assertions.assertEquals("/path/to/thing", path)
    }

    @Test
    fun normalize_path_9() {
        val path = UriUtils.normalizePath("http://host/path/to/./thing")
        Assertions.assertEquals("http://host/path/to/thing", path)
    }


}