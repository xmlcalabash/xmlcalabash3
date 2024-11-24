package com.xmlcalabash.io

import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.message.BasicHeader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MimeReader(val stream: InputStream, val boundary: String) {
    private val H_NAME = 1
    private val H_VALUE = 2
    private val H_DONE = 3
    private val B_SOL = 4
    private val B_CR = 5
    private val B_MATCHSEP = 6
    private val B_DATA = 7
    private val B_MATCHLAST = 8

    private val separator = "--${boundary}"
    private val lastsep = "${separator}--"
    private var _peek = -1
    private var _headers = mutableListOf<Header>()

    val headers: List<Header>
        get() = _headers

    init {
        val line = getLine()
        if (line != separator) {
            throw RuntimeException("MIME multipart doesn't start with separator?")
        }
    }

    fun readHeaders(): Boolean {
        _headers.clear()
        if (peekByte() < 0) {
            return false
        }

        var header = getHeader()
        while (header != null) {
            _headers.add(header)
            header = getHeader()
        }

        return true
    }

    fun header(name: String): Header? {
        val lcname = name.lowercase()
        for (header in _headers) {
            if (header.name.lowercase() == lcname) {
                return header
            }
        }
        return null
    }

    fun readBodyPart(contentLength: Long): InputStream {
        val bytes = ByteArrayOutputStream()
        var bytesLeft = contentLength

        if (_peek >= 0) {
            bytes.write(_peek)
            _peek = -1
            bytesLeft -= 1
        }

        while (bytesLeft > 0) {
            val maxread = Math.min(bytesLeft, 16384).toInt()
            val tmp = ByteArray(maxread)
            val len = stream.read(tmp)
            if (len < 0) {
                throw RuntimeException("Got -1 reading stream?")
            }
            bytes.write(tmp, 0, len)
            bytesLeft -= len
        }

        // The separator has to be at the start of a line
        var peek = peekByte()
        if (peek == '\r'.code) {
            nextByte()
            peek = peekByte()
        }
        if (peek == '\n'.code) {
            nextByte()
        }

        val line = getLine()
        if (line != separator && line != lastsep) {
            throw RuntimeException("MIME multipart missing separator?")
        }

        return ByteArrayInputStream(bytes.toByteArray())
    }

    fun readBodyPart(): InputStream {
        val bodygrow = 32768
        var bodysize = bodygrow * 4
        var bodybytes = ByteArray(bodysize)
        var bodyidx = 0
        var sepidx = 0
        var done = false
        var state = B_SOL

        while (!done) {
            val b = nextByte()
            if (b < 0) {
                throw RuntimeException("Got -1 reading stream?")
            }

            if (bodyidx == bodysize) {
                // Why am I managing these buffers myself?
                val newbytes = ByteArray(bodysize + bodygrow)
                for (idx in 0 ..< bodysize) {
                    newbytes[idx] = bodybytes[idx]
                }
                bodybytes = newbytes
                bodysize += bodygrow
            }

            bodybytes[bodyidx++] = b.toByte()

            when (state) {
                B_SOL, B_MATCHSEP -> {
                    if (sepidx == separator.length) {
                        when (b) {
                            '-'.code -> {
                                state = B_MATCHLAST
                                nextByte()
                            }
                            '\r'.code, '\n'.code -> {
                                done = true
                                bodyidx -= (separator.length + 3) // The CR/LF is part of the separator
                                if (b == '\r'.code && peekByte() == '\n'.code) {
                                    nextByte()
                                }
                            }
                            else -> {
                                state = B_DATA
                                sepidx = 0
                            }
                        }
                    } else {
                        if (b.toChar() == separator[sepidx]) {
                            state = B_MATCHSEP
                            sepidx += 1
                        } else {
                            sepidx = 0
                            if (b == '\n'.code) {
                                state = B_SOL
                            } else {
                                state = B_DATA
                            }
                        }
                    }

                }
                B_MATCHLAST -> {
                    if (b == '\r'.code || b == '\n'.code) {
                        done = true
                        // +4 = +1 for the CR or LF, +1 for the "-" that got added, and +2 for the CR/LF
                        bodyidx -= (separator.length + 4) // The CR/LF is part of the separator
                        if (b == '\r'.code && peekByte() == '\n'.code) {
                            nextByte()
                        }
                    } else {
                        state = B_DATA
                        sepidx = 0
                    }
                }
                B_CR, B_DATA -> {
                    if (b == '\n'.code) {
                        state = B_SOL
                    }
                }
            }

            if (b == '\r'.code) {
                state = B_CR
            }
        }

        return ByteArrayInputStream(bodybytes, 0, bodyidx)
    }

    private fun getLine(): String {
        var line = ""
        var done = false

        while (!done) {
            var b = nextByte()
            var peek = peekByte()

            if (b == '\r'.code) {
                if (peek == '\n'.code) {
                    b = nextByte()
                    peek = peekByte()
                } else {
                    b = '\n'.code
                }
            }

            if (b < 0) {
                throw RuntimeException("Got -1 reading stream")
            }

            if (b == '\n'.code) {
                done = true
            } else {
                line += b.toChar()
            }
        }

        return line
    }

    // RFC 822 suggests that headers must be separated by CRLF, but in practice
    // it seems that LF alone is sometimes used. I suppose CR alone is possible too.
    private fun getHeader(): Header? {
        var name = ""
        var value = ""

        var state = H_NAME
        while (state != H_DONE) {
            var b = nextByte()
            var peek = peekByte()

            if (b == '\r'.code) {
                if (peek == '\n'.code) {
                    b = nextByte()
                    peek = peekByte()
                } else {
                    b = '\n'.code
                }
            }

            if (b < 0) {
                throw RuntimeException("Got -1 reading stream")
            }

            when (state) {
                H_NAME -> {
                    if (b == '\n'.code) {
                        state = H_DONE
                    } else {
                        if (b == ':'.code) {
                            state = H_VALUE
                        } else {
                            name += b.toChar()
                        }
                    }
                }
                H_VALUE -> {
                    if (b == '\n'.code) {
                        if (peek == ' '.code || peek == '\t'.code) {
                            // nop; we'll catch this in the next loop
                        } else {
                            state = H_DONE
                        }
                    } else {
                        value += b.toChar()
                    }
                }
                else -> {
                    throw RuntimeException("Invalid state in getHeader()?")
                }
            }
        }

        if (name == "") {
            return null
        } else {
            return BasicHeader(name.trim(), value.trim())
        }
    }

    private fun peekByte(): Int {
        if (_peek < 0) {
            _peek = stream.read()
        }
        return _peek
    }

    private fun nextByte(): Int {
        if (_peek >= 0) {
            val v = _peek
            _peek = -1
            return v
        } else {
            return stream.read()
        }
    }
}