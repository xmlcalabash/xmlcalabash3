package com.xmlcalabash.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import org.apache.http.Header
import org.apache.http.message.BasicHeader

import scala.collection.mutable.ListBuffer

class MIMEReader(val stream: InputStream, val boundary: String) {
  private val H_NAME = 1
  private val H_VALUE = 2
  private val H_DONE = 3
  private val B_SOL = 4
  private val B_CR = 5
  private val B_MATCHSEP = 6
  private val B_DATA = 7
  private val B_MATCHLAST = 8

  private val separator = s"--$boundary"
  private val lastsep = s"$separator--"
  private var _peek = -1
  private var headers = ListBuffer.empty[Header]

  if (getLine() != separator) {
    throw new RuntimeException("MIME multipart doesn't start with separator?")
  }

  def readHeaders(): Boolean = {
    headers.clear()
    if (peekByte() < 0) {
      return false
    }

    var h = getHeader()
    while (h.isDefined) {
      headers += h.get
      h = getHeader()
    }

    true
  }

  def getHeaders: List[Header] = headers.toList

  def header(name: String): Option[Header] = {
    val lcname = name.toLowerCase()
    for (header <- headers) {
      if (header.getName.toLowerCase() == lcname) {
        return Some(header)
      }
    }
    None
  }

  def readBodyPart(contentLength: Long): InputStream = {
    val bytes = new ByteArrayOutputStream()
    var bytesLeft = contentLength

    if (_peek >= 0) {
      bytes.write(_peek)
      _peek = -1
      bytesLeft -= 1
    }

    while (bytesLeft > 0) {
      val maxread = Math.min(bytesLeft, 16384).toInt
      val tmp = new Array[Byte](maxread)
      val len = stream.read(tmp)
      if (len < 0) {
        throw new RuntimeException("Got -1 reading stream?")
      }
      bytes.write(tmp, 0, len)
      bytesLeft -= len
    }

    // The separator has to be at the start of a line
    var peek = peekByte()
    if (peek == '\r') {
      nextByte()
      peek = peekByte()
    }
    if (peek == '\n') {
      nextByte()
    }

    val line = getLine()
    if (line != separator && line != lastsep) {
      throw new RuntimeException("MIME multipart missing separator?")
    }

    new ByteArrayInputStream(bytes.toByteArray)
  }

  def readBodyPart(): InputStream = {
    val bodygrow = 32768
    var bodysize = bodygrow * 4
    var bodybytes = new Array[Byte](bodysize)
    var bodyidx = 0
    var sepidx = 0
    var done = false
    var state = B_SOL

    while (!done) {
      val b = nextByte()
      if (b < 0) {
        throw new RuntimeException("Got -1 reading stream?")
      }

      if (bodyidx == bodysize) {
        val newbytes = new Array[Byte](bodysize + bodygrow)
        Array.copy(bodybytes, 0, newbytes, 0, bodysize)
        bodybytes = newbytes
        bodysize += bodygrow
      }

      bodybytes(bodyidx) = b.asInstanceOf[Byte]
      bodyidx += 1

      state match {
        case B_SOL | B_MATCHSEP =>
          if (sepidx == separator.length) {
            b match {
              case '-' =>
                state = B_MATCHLAST
                nextByte()
              case '\r' | '\n' =>
                done = true
                bodyidx -= (separator.length + 3) // The CR/LF is part of the separator
                if (b == '\r' && peekByte() == '\n') {
                  nextByte()
                }
              case _ =>
                state = B_DATA
                sepidx = 0
            }
          } else {
            if (b.asInstanceOf[Char] == separator.charAt(sepidx)) {
              state = B_MATCHSEP
              sepidx += 1
            } else {
              sepidx = 0
              if (b == '\n') {
                state = B_SOL
              } else {
                state = B_DATA
              }
            }
          }
        case B_MATCHLAST =>
          if (b == '\r' || b == '\n') {
            done = true
            // +4 = +1 for the CR or LF, +1 for the "-" that got added, and +2 for the CR/LF
            bodyidx -= (separator.length + 4) // The CR/LF is part of the separator
            if (b == '\r' && peekByte() == '\n') {
              nextByte()
            }
          } else {
            state = B_DATA
            sepidx = 0
          }
        case B_CR | B_DATA =>
          if (b == '\n') {
            state = B_SOL
          }
      }

      if (b == '\r') {
        state = B_CR
      }
    }

    new ByteArrayInputStream(bodybytes, 0, bodyidx)
  }

  private def getLine(): String = {
    var line = ""
    var done = false

    while (!done) {
      var b = nextByte()
      var peek = peekByte()

      if (b == '\r') {
        if (peek == '\n') {
          b = nextByte()
          peek = peekByte()
        } else {
          b = '\n'
        }
      }

      if (b < 0) {
        throw new RuntimeException("Got -1 reading stream")
      }

      if (b == '\n') {
        done = true
      } else {
        line += b.asInstanceOf[Char]
      }
    }

    line
  }

  // RFC 822 suggests that headers must be separated by CRLF, but in practice
  // it seems that LF alone is sometimes used. I suppose CR alone is possible too.
  private def getHeader(): Option[Header] = {
    var name = ""
    var value = ""

    var state = H_NAME
    while (state != H_DONE) {
      var b = nextByte()
      var peek = peekByte()

      if (b == '\r') {
        if (peek == '\n') {
          b = nextByte()
          peek = peekByte()
        } else {
          b = '\n'
        }
      }

      if (b < 0) {
        throw new RuntimeException("Got -1 reading stream")
      }

      state match {
        case H_NAME =>
          if (b == '\n') {
            state = H_DONE
          } else {
            if (b == ':') {
              state = H_VALUE
            } else {
              name += b.asInstanceOf[Char]
            }
          }
        case H_VALUE =>
          if (b == '\n') {
            if (peek == ' ' || peek == '\t') {
              // nop; we'll catch this in the next loop
            } else {
              state = H_DONE
            }
          } else {
            value += b.asInstanceOf[Char]
          }
        case _ =>
          throw new RuntimeException("Invalid state in getHeader()?")
      }
    }

    if (name == "") {
      None
    } else {
      Some(new BasicHeader(name.trim(), value.trim()))
    }
  }

  private def peekByte(): Int = {
    if (_peek < 0) {
      _peek = stream.read()
    }
    _peek
  }

  private def nextByte(): Int = {
    if (_peek >= 0) {
      val v = _peek
      _peek = -1
      v
    } else {
      stream.read()
    }
  }
}
