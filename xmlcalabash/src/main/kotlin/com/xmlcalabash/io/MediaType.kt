package com.xmlcalabash.io

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.util.MediaClassification
import net.sf.saxon.s9api.XdmAtomicValue
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern

class MediaType private constructor(val mediaType: String, val mediaSubtype: String,
                                    val suffix: String? = null, val inclusive: Boolean = true,
                                    val parameters: List<MediaTypeParameter> = listOf()) {
    companion object {
        val ANY = MediaType("*", "*")
        val GZIP = MediaType("application", "gzip")
        val HTML = MediaType("text", "html")
        val JPEG = MediaType("image", "jpeg")
        val JSON = MediaType("application", "json")
        val MULTIPART = MediaType("multipart", "*")
        val MULTIPART_MIXED = MediaType("multipart", "mixed")
        val OCTET_STREAM = MediaType("application", "octet-stream")
        val PDF = MediaType("application", "pdf")
        val PNG = MediaType("image", "png")
        val TEXT = MediaType("text", "plain")
        val XHTML = MediaType("application", "xhtml", "xml")
        val XML  = MediaType("application", "xml")
        val SVG = MediaType("image", "svg", "xml")
        val XQUERY = MediaType("application", "xquery")
        val XSLT = MediaType("application", "xslt", "xml")
        val YAML = MediaType("application", "vnd.yaml")
        val TOML = MediaType("application", "toml")
        val CSV = MediaType("text", "csv")
        val ZIP = MediaType("application", "zip")
        val JAR = MediaType("application", "java-archive")
        val TAR = MediaType("application", "x-tar")
        val AR = MediaType("application", "x-archive")
        val ARJ = MediaType("application", "x-arj")
        val CPIO = MediaType("application", "x-cpio")
        val SEVENZ = MediaType("application", "x-7z-compressed")
        val JAVA_PROPERTIES = MediaType("text", "x-java-properties")

        val JSONLD = MediaType("application", "ld", "json")
        val N3 = MediaType("text", "n3")
        val NQUADS = MediaType("application", "n-quads")
        val NTRIPLES = MediaType("application", "n-triples")
        val RDFJSON = MediaType("application", "rdf", "json")
        val RDFTHRIFT = MediaType("application", "rdf", "thrift")
        val RDFXML = MediaType("application", "rdf", "xml")
        val SPARQL = MediaType("application", "sparql-query")
        val SPARQL_RESULTS_XML = MediaType("application", "sparql-results", "xml")
        val SPARQL_RESULTS_JSON = MediaType("application", "sparql-results", "json")
        val TRIG = MediaType("application", "trig")
        val TRIX = MediaType("application", "trix", "xml")
        val TURTLE = MediaType("text", "turtle")

        val XML_OR_HTML = listOf<MediaType>(
            MediaType("application", "xml"),
            MediaType("text", "xml"),
            MediaType("*", "*", "xml")
        )

        val MATCH_XML = listOf<MediaType>(
            MediaType("application", "xml"),
            MediaType("text", "xml"),
            MediaType("*", "*", "xml"),
            MediaType("application", "xhtml", "xml", inclusive = false)
        )

        val MATCH_NOT_XML = listOf<MediaType>(
            MediaType("application", "xml", inclusive = false),
            MediaType("text", "xml", inclusive = false),
            MediaType("*", "*", "xml", inclusive = false)
        )

        val MATCH_HTML = listOf<MediaType>(
            MediaType("text", "html"),
            MediaType("application", "xhtml", "xml")
        )

        val MATCH_NOT_HTML = listOf<MediaType>(
            MediaType("text", "html", inclusive = false),
            MediaType("application", "xhtml", "xml", inclusive = false),
        )

        val MATCH_TEXT = listOf<MediaType>(
            MediaType("text", "*"),
            MediaType("application", "relax-ng-compact-syntax"),
            MediaType("application", "xquery"),
            MediaType("application", "javascript"),
            MediaType("application", "trig"),
            MediaType("application", "n-triples"),
            MediaType("application", "n-quads"),
            MediaType("application", "sparql-query"),
            MediaType("text", "html", inclusive = false),
            MediaType("text", "xml", inclusive = false),
            MediaType("text", "yaml", inclusive = false)
        )

        val MATCH_NOT_TEXT = listOf<MediaType>(
            MediaType("text", "*", inclusive = false),
            MediaType("application", "relax-ng-compact-syntax", inclusive = false),
            MediaType("application", "xquery", inclusive = false),
            MediaType("application", "javascript", inclusive = false),
        )

        val MATCH_JSON = listOf<MediaType>(
            MediaType("application", "json"),
            MediaType("*", "*", "json")
        )

        val MATCH_NOT_JSON = listOf<MediaType>(
            MediaType("application", "json", inclusive = false)
        )

        val MATCH_YAML = listOf<MediaType>(
            MediaType("application", "vnd.yaml"),
            MediaType("text", "yaml"),
            MediaType("application", "yaml"),
            MediaType("application", "x-yaml"),
            MediaType("application", "*", "yaml"),
        )

        val MATCH_TOML = listOf<MediaType>(
            MediaType("application", "toml")
        )

        val MATCH_CSV = listOf<MediaType>(
            MediaType("text", "csv")
        )

        val MATCH_ANY = listOf<MediaType>(
            MediaType("*", "*")
        )

        val MATCH_NOT_ANY = listOf<MediaType>(
            MediaType("*", "*", inclusive = false)
        )

        val classifications = listOf<Pair<MediaClassification, List<MediaType>>> (
            Pair(MediaClassification.XHTML, listOf(XHTML)),
            Pair(MediaClassification.HTML, MATCH_HTML),
            Pair(MediaClassification.XML, MATCH_XML),
            Pair(MediaClassification.JSON, MATCH_JSON),
            Pair(MediaClassification.YAML, MATCH_YAML),
            Pair(MediaClassification.TOML, MATCH_TOML),
            Pair(MediaClassification.TEXT, MATCH_TEXT),
        )

        fun parse(mtype: String?): MediaType? {
            if (mtype == null) {
                return null
            } else {
                return parse(mtype)
            }
        }

        fun parse(mtype: String, forceEncoding: String? = null): MediaType {
            var ctype = parseMatch(mtype, forceEncoding)
            if (!"[A-Za-z0-9][-A-Za-z0-9!#\$&0^_\\\\.+]*".toRegex().matches(ctype.mediaType) || ctype.mediaType.length > 127) {
                throw XProcError.xdInvalidContentType(mtype).exception()
            }
            if (!"[A-Za-z0-9][-A-Za-z0-9!#\$&0^_\\\\.+]*".toRegex().matches(ctype.mediaSubtype) || ctype.mediaSubtype.length > 127) {
                throw XProcError.xdInvalidContentType(mtype).exception()
            }
            return ctype
        }

        fun parseMatch(mtype: String, forceEncoding: String? = null): MediaType {
            // [-]type/subtype+ext; name1=val1; name2=val2
            var pos = mtype.indexOf("/")
            if (pos < 0) {
                throw XProcError.xdInvalidContentType(mtype).exception()
            }

            var mediaType = mtype.substring(0, pos).trim()
            var rest = mtype.substring(pos + 1).trim()
            var inclusive = true
            if (mediaType.startsWith("-")) {
                inclusive = false
                mediaType = mediaType.substring(1)
            }

            // This is a bit convoluted because of the way 'rest' is reused.
            // There was a bug and this was the easiest fix. #hack
            var params = ""
            pos = rest.indexOf(";")
            if (pos >= 0) {
                params = rest.substring(pos+1)
                rest = rest.substring(0, pos).trim()
            }

            var mediaSubtype = rest
            var suffix: String? = null

            if (mediaSubtype.contains("+")) {
                pos = mediaSubtype.indexOf("+")
                suffix = mediaSubtype.substring(pos+1).trim()
                mediaSubtype = mediaSubtype.substring(0, pos).trim()
            }

            if (forceEncoding == null && params == "") {
                return MediaType(mediaType, mediaSubtype, suffix, inclusive = inclusive)
            }

            rest = params
            pos = rest.indexOf(";")
            val plist = mutableListOf<MediaTypeParameter>()
            if (forceEncoding != null) {
                plist.add(MediaTypeParameter("charset=" + forceEncoding))
            }

            while (pos >= 0) {
                val param = rest.substring(0, pos).trim()
                rest = rest.substring(pos+1)
                if (param != "") {
                    if (forceEncoding == null || !param.startsWith("charset=")) {
                        plist.add(MediaTypeParameter(param))
                    }
                }
                pos = rest.indexOf(";")
            }

            if (rest.isNotBlank()) {
                rest = rest.trim()
                if (forceEncoding == null || !rest.startsWith("charset=")) {
                    plist.add(MediaTypeParameter(rest))
                }
            }

            return MediaType(mediaType, mediaSubtype, suffix, inclusive, plist.toList())
        }

        fun parseList(ctypes: String): List<MediaType> {
            val mlist = mutableListOf<MediaType>()
            for (ctype in ctypes.split("\\s+".toRegex())) {
                val excl = ctype[0] == '-'
                val type = if (excl) {
                    ctype.substring(1)
                } else {
                    ctype
                }

                when (type) {
                    "xml" -> if (excl) mlist.addAll(MATCH_NOT_XML) else mlist.addAll(MATCH_XML)
                    "html" -> if (excl) mlist.addAll(MATCH_NOT_HTML) else mlist.addAll(MATCH_HTML)
                    "text" -> if (excl) mlist.addAll(MATCH_NOT_TEXT) else mlist.addAll(MATCH_TEXT)
                    "json" -> if (excl) mlist.addAll(MATCH_NOT_JSON) else mlist.addAll(MATCH_JSON)
                    "any" -> if (excl) mlist.addAll(MATCH_NOT_ANY) else mlist.addAll(MATCH_ANY)
                    else -> {
                        if (ctype.contains("/")) {
                            mlist.add(parse(ctype))
                        } else {
                            throw XProcError.xsInvalidContentType(ctype).exception()
                        }
                    }
                }
            }
            return mlist
        }
    }

    fun discardParameters(): MediaType {
        if (parameters.isEmpty()) {
            return this
        }
        return MediaType(mediaType, mediaSubtype, suffix, inclusive)
    }

    fun discardParameters(paramNames: List<String>): MediaType {
        var discarded = false
        val newParams = mutableListOf<MediaTypeParameter>()
        for (param in parameters) {
            val name = param.name
            if (paramNames.contains(name)) {
                discarded = true
            } else {
                newParams.add(param)
            }
        }
        if (!discarded) {
            return this
        }
        return MediaType(mediaType, mediaSubtype, suffix, inclusive, newParams.toList())
    }

    fun addParam(name: String, value: String): MediaType {
        val newParams = mutableListOf<MediaTypeParameter>()
        for (param in parameters) {
            if (param.name != name) {
                newParams.add(param)
            }
        }
        newParams.add(MediaTypeParameter(name, value))
        return MediaType(mediaType, mediaSubtype, suffix, inclusive, newParams.toList())
    }

    fun matchingMediaType(mtypes: List<MediaType>): MediaType? {
        var match: MediaType? = null

        // Go through the whole list in case there are exclusions as well as inclusions
        for (mtype in mtypes) {
            if (matches(mtype)) {
                match = mtype
            }
        }

        return match
    }

    fun matches(mtype: MediaType): Boolean  {
        if (mtype.mediaType == "application" && mtype.mediaSubtype == "octet-stream") {
            return true
        }

        // application/xml should match */*
        // but text/plain shouldn't match */*+xml
/*
        // I wonder how many special cases there really are...can this be generalized somehow?
        if (testMatching(mtype, "xml", "xml")) {
            return true
        }
        if (testMatching(mtype, "json", "json")) {
            return true
        }
*/
        var mmatch = mediaType == mtype.mediaType || mtype.mediaType == "*"
        mmatch = mmatch && (mediaSubtype == mtype.mediaSubtype || mtype.mediaSubtype == "*")
        mmatch = mmatch && (mtype.suffix == null || suffix == mtype.suffix)
        return mmatch
    }

    private fun testMatching(mtype: MediaType, hassubtype: String, hassuffix: String): Boolean {
        if ((mediaType == "text" || mediaType == "application") && (mediaSubtype == hassubtype || suffix == hassuffix)) {
            if ((mtype.mediaType == "text" || mtype.mediaType == "application")
                && (mtype.mediaSubtype == hassubtype || mtype.suffix == hassuffix)) {
                return true
            }
        }
        return false
    }

    fun classification(): MediaClassification {
        for ((type, typeList) in classifications) {
            val mtype = matchingMediaType(typeList)
            if (mtype != null && mtype.inclusive) {
                return type
            }
        }
        return MediaClassification.BINARY
    }

    fun allowed(types: List<MediaType>): Boolean {
        // This media type is allowed if it's allowed by at least one member of types
        // and is not forbidden by the last member
        var allowed = false
        for (ctype in types) {
            if (matches(ctype)) {
                allowed = ctype.inclusive
            }
        }

        return allowed
    }

    fun paramValue(name: String): String? {
        for (param in parameters) {
            if (param.name == name) {
                val value = param.value
                if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                    return value.substring(1, value.length - 1)
                }
                return value
            }
        }
        return null
    }

    fun charset(): Charset? {
        val name = paramValue("charset")
        if (name == null) {
            return null
        }

        if (!Charset.isSupported(name)) {
            throw XProcError.xdUnsupportedDocumentCharset(name).exception()
        }

        return Charset.forName(name)
    }

    fun assertValid(): MediaType {
        if (assertValidName(mediaType) && assertValidName(mediaSubtype)) {
            return this
        }
        throw RuntimeException("Unrecognized content type: ${mediaType}/${mediaSubtype}")
    }

    private fun assertValidName(literal: String): Boolean {
        val name = literal.lowercase(Locale.getDefault())
        val lengthOk = name.isNotEmpty() && name.length <= 127
        val startOk = lengthOk && name.substring(0,1).matches("^[a-z0-9]".toRegex())
        val contentOk = startOk && name.matches("[a-z0-9${Pattern.quote("!#$&-^_.+")}]+$$".toRegex())
        return contentOk
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is MediaType -> {
                if (mediaType != other.mediaType || mediaSubtype != other.mediaSubtype) {
                    return false
                }

                return suffix == other.suffix
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return (41 * mediaType.hashCode()) + mediaSubtype.hashCode()
    }

    fun toAtomicValue(): XdmAtomicValue {
        return XdmAtomicValue(toString())
    }

    fun toStringWithoutParameters(): String {
        if (suffix == null) {
            return "${mediaType}/${mediaSubtype}"
        }
        return "${mediaType}/${mediaSubtype}+${suffix}"
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (!inclusive) {
            sb.append("-")
        }
        sb.append(toStringWithoutParameters())

        for (param in parameters) {
            // Space after ; for compatibility with Morgana XProc results
            sb.append("; ").append(param)
        }

        return sb.toString()
    }
}
