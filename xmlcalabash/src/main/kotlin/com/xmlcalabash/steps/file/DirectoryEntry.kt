package com.xmlcalabash.steps.file

import java.io.File

abstract class DirectoryEntry(val file: File, var include: Boolean = false) {
    override fun toString(): String {
        return file.toString()
    }
}