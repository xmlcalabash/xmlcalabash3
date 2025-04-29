package com.xmlcalabash.test

import com.xmlcalabash.util.Urify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

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
}