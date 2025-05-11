package com.xmlcalabash.steps.file

import java.io.File

class DirectoryDir(file: File, include: Boolean): DirectoryEntry(file, include) {
    val entries = mutableListOf<DirectoryEntry>()
    override fun toString(): String {
        return "${file} (${entries.size})"
    }
}