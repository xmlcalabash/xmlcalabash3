package com.xmlcalabash.test

import com.xmlcalabash.util.UriUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI

class UriTest {
    @Test
    fun relativeTo_diffScheme() {
        val base = URI("http://host/path/to/thing/baseName")
        val relative = URI("file:/path/to/thing/subdir/relativeName")
        val result = UriUtils.makeRelativeTo(base, relative)
        Assertions.assertEquals(URI("file:/path/to/thing/subdir/relativeName"), result)
    }

    @Test
    fun relativeTo_diffAuthority() {
        val base = URI("http://host/path/to/thing/baseName")
        val relative = URI("http://otherhost/path/to/thing/subdir/relativeName")
        val result = UriUtils.makeRelativeTo(base, relative)
        Assertions.assertEquals(URI("http://otherhost/path/to/thing/subdir/relativeName"), result)
    }

    @Test
    fun relativeTo_same() {
        val base = URI("file:/path/to/thing/baseName")
        val relative = URI("file:/path/to/thing/relativeName")
        val result = UriUtils.makeRelativeTo(base, relative)
        Assertions.assertEquals(URI("relativeName"), result)
    }

    @Test
    fun relativeTo_subdir() {
        val base = URI("file:/path/to/thing/baseName")
        val relative = URI("file:/path/to/thing/subdir/relativeName")
        val result = UriUtils.makeRelativeTo(base, relative)
        Assertions.assertEquals(URI("subdir/relativeName"), result)
    }

    @Test
    fun relativeTo_dotdotSubdir() {
        val base = URI("file:/path/to/thing/baseName")
        val relative = URI("file:/path/to/relativeName")
        val result = UriUtils.makeRelativeTo(base, relative)
        Assertions.assertEquals(URI("../relativeName"), result)
    }

    @Test
    fun relativeTo_dotdot2Subdir() {
        val base = URI("file:/path/to/thing/baseName")
        val relative = URI("file:/path/relativeName")
        val result = UriUtils.makeRelativeTo(base, relative)
        Assertions.assertEquals(URI("../../relativeName"), result)
    }

}