package com.xmlcalabash.app

import com.xmlcalabash.exceptions.XProcError
import java.io.File

/**
 * Describe how output filenames should be constructed.
 *
 * This method returns a sequence of filenames from a pattern. Within the pattern, a percent sign (%)
 * followed by zero or more digits, followed by a latin small letter o (o), a latin small
 * letter d (d), a latin small letter x (x), or a latin capital letter x (X) will be replaced with
 * a sequence number. Files are numbered from 1.
 *
 * If the trailing letter is "o", the number will be formatted in octal, if the trailing digit is "d",
 * the number will be formatted in decimal, if the trailing letter is "x" or "X", the number will
 * be formatted in hexadecimal with either lower- or upper-case alphabetic digits.
 *
 * If one or more leading digits are provided, the number will be padded to that length.
 *
 * If "%%" appears in the pattern, it will be replaced by a single "%" in the generated filename.
 *
 * In other words the pattern `hello%%world%03x.xml` will produce a sequence of filenames:
 * `hello%world001.xml`, `hello%world002.xml`, ... `hello%worldfff.xml`, `hello%world1000.xml`, etc.
 *
 * The result of performing the sequential substutition on the pattern string must result in a valid filename for
 * the platform where the process is running.
 *
 * @param pattern the pattern string.
 */
class OutputFilename(val pattern: String) {
    private val regex = "(%%)|(%[0-9]*[odxX]?)".toRegex()
    private var nextId = 1
    private var currentFilename: String = pattern

    /**
     * Is this output filename going to produce a sequence of names?
     *
     * If the input pattern does not contain any substrings that can be used for sequential
     * numbering, the output filename is not sequential.
     */
    fun isSequential(): Boolean {
        var result: MatchResult? = regex.find(pattern) ?: return false

        if (result!!.value != "%%") {
            return true
        }

        result = result.next()
        while (result != null) {
            if (result.value != "%%") {
                return true
            }
            result = result.next()
        }

        return false
    }

    /**
     * Get the next file in the sequence.
     *
     * If [isSequential] is false, the same name will be returned each time.
     */
    fun nextFile(): File {
        currentFilename = pattern
        var result: MatchResult? = regex.find(pattern)
        if (result != null) {
            val sb = StringBuilder()
            if (result.range.first > 0) {
                sb.append(pattern.substring(0, result.range.first))
            }
            if (result.value == "%%") {
                sb.append("%")
            } else {
                sb.append(String.format(result.value, nextId))
            }

            var nextPos = result.range.last + 1
            result = result.next()
            while (result != null) {
                if (result.range.first > nextPos) {
                    sb.append(pattern.substring(nextPos, result.range.first))
                }
                if (result.value == "%%") {
                    sb.append("%")
                } else {
                    sb.append(String.format(result.value, nextId))
                }
                nextPos = result.range.last + 1
                result = result.next()
            }

            if (nextPos < pattern.length) {
                sb.append(pattern.substring(nextPos))
            }

            currentFilename = sb.toString()
        }

        nextId++
        val file = File(currentFilename)
        if (file.exists() && file.isDirectory) {
            throw XProcError.xiUnwritableOutputFile(currentFilename).exception()
        }
        if (file.exists() && !file.canWrite()) {
            throw XProcError.xiUnwritableOutputFile(currentFilename).exception()
        }

        return file
    }
}