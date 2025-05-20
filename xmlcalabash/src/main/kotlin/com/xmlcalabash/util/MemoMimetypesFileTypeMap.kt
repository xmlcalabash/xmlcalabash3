package com.xmlcalabash.util

import javax.activation.MimetypesFileTypeMap

class MemoMimetypesFileTypeMap(): MimetypesFileTypeMap() {
    private val _extensionMap = mutableMapOf<String, String>()
    val extensionMap: Map<String, String>
        get() = _extensionMap

    override fun addMimeTypes(mime_types: String?) {
        super.addMimeTypes(mime_types)
        if (mime_types == null) {
            return
        }
        val tokens = mutableListOf<String>()
        tokens.addAll(mime_types.split("\\s+".toRegex()))
        val ctype = tokens.removeFirstOrNull()
        if (ctype != null) {
            for (ext in tokens) {
                val dotless = ext.trimStart('.')
                if (dotless != "") {
                    _extensionMap.put(ext, ctype)
                }
            }
        }
    }
}