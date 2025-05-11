package com.xmlcalabash.steps.file

import com.xmlcalabash.io.MediaType
import java.io.File

class DirectoryFile(file: File, include: Boolean, val contentType: MediaType): DirectoryEntry(file, include) {
}