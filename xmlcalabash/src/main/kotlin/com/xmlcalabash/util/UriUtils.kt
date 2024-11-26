package com.xmlcalabash.util

import java.net.URI
import java.nio.file.Paths

class UriUtils {
    companion object {
        fun homeAsUri(): URI = dirAsUri(System.getProperty("user.home"))
        fun cwdAsUri(): URI = dirAsUri(System.getProperty("user.dir"))
        fun dirAsUri(dir: String): URI {
            return Paths.get(dir).toUri()
        }
        fun makeRelativeTo(base: URI, relative: URI): URI {
            if (relative.isOpaque || base.scheme != relative.scheme || base.authority != relative.authority) {
                return relative
            }

            val normParts = mutableListOf<String>()
            val relativeParts = mutableListOf<String>()

            normParts.addAll(base.normalize().path.split("/"))
            relativeParts.addAll(relative.normalize().path.split("/"))

            normParts.removeLast()
            val lastPart = relativeParts.removeLast()

            while (normParts.isNotEmpty() && relativeParts.isNotEmpty() && normParts.first() == relativeParts.first()) {
                normParts.removeFirst()
                relativeParts.removeFirst()
            }

            if (normParts.isEmpty()) {
                if (relativeParts.isEmpty()) {
                    return URI(lastPart)
                }
                return URI(relativeParts.joinToString("/") + "/" + lastPart)
            }

            val builder = StringBuilder()
            for (part in normParts) {
                builder.append("../")
            }
            builder.append(relativeParts.joinToString("/"))
            builder.append(lastPart)

            return URI(builder.toString())
        }
    }
}