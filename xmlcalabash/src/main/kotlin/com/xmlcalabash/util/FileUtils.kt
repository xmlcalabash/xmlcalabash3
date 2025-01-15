package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcError
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.toPath

class FileUtils {
    companion object {
        fun outputStream(uri: URI): OutputStream {
            val target = UriUtils.cwdAsUri().resolve(uri)
            if (target.scheme != "file") {
                throw XProcError.xdIsNotWriteable(target.toString(), "Only file: URIs are supported for output").exception()
            }
            val file = target.toPath().toFile()
            val parent = file.parentFile
            parent.mkdirs()
            return FileOutputStream(file)
        }

        fun outputStream(path: String): OutputStream {
            return outputStream(UriUtils.cwdAsUri().resolve(path))
        }

        fun outputStream(file: File): OutputStream {
            file.parentFile.mkdirs()
            return FileOutputStream(file)
        }

        fun outputStream(path: Path): OutputStream {
            return outputStream(path.toFile())
        }
    }
}