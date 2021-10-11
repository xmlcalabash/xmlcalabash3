package com.xmlcalabash.parsers

// This file was generated on Thu Mar 18, 2021 17:31 (UTC) by REx v5.52 which is Copyright (c) 1979-2020 by Gunther Rademacher <grd@gmx.net>
// REx command line: SequenceParser.ebnf -ll 2 -scala -tree

import collection.mutable.ArrayBuffer

class SequenceParser {

  def this(string: String, eh: SequenceParser.EventHandler) = {
    this
    initialize(string, eh)
  }

  def initialize(source: String, parsingEventHandler: SequenceParser.EventHandler): Unit = {
    eventHandler = parsingEventHandler
    input = source
    size = source.length
    reset(0, 0, 0)
  }

  def getInput: String = {
    return input
  }

  def getTokenOffset: Int = {
    return b0
  }

  def getTokenEnd: Int = {
    return e0
  }

  def reset(l: Int, b: Int, e: Int): Unit = {
            b0 = b; e0 = b
    l1 = l; b1 = b; e1 = e
    end = e
    eventHandler.reset(input)
  }

  def reset: Unit = {
    reset(0, 0, 0)
  }

  def parse_Sequence: Unit = {
    eventHandler.startNonterminal("Sequence", e0)
    lookahead1W(3)                  // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | PrefixedName | UnprefixedName | S^WS | '(' | '(:'
    l1 match {
    case 10 =>                      // '('
      consume(10)                   // '('
      lookahead1W(4)                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | PrefixedName | UnprefixedName | S^WS | '(:' | ')'
      if (l1 != 12) {               // ')'
        whitespace
        parse_Item
        var c1 = true
        while (c1) {
          lookahead1W(1)            // S^WS | '(:' | ')' | ','
          if (l1 != 13) {           // ','
            c1 = false
          }
          else {
            consume(13)             // ','
            lookahead1W(2)          // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | PrefixedName | UnprefixedName | S^WS | '(:'
            whitespace
            parse_Item
          }
        }
      }
      consume(12)                   // ')'
    case _ =>
      parse_Item
    }
    eventHandler.endNonterminal("Sequence", e0)
  }

  private def parse_Item: Unit = {
    eventHandler.startNonterminal("Item", e0)
    l1 match {
    case 1 =>                       // IntegerLiteral
      consume(1)                    // IntegerLiteral
    case 2 =>                       // DecimalLiteral
      consume(2)                    // DecimalLiteral
    case 3 =>                       // DoubleLiteral
      consume(3)                    // DoubleLiteral
    case 4 =>                       // StringLiteral
      consume(4)                    // StringLiteral
    case 5 =>                       // URIQualifiedName
      consume(5)                    // URIQualifiedName
    case 6 =>                       // PrefixedName
      consume(6)                    // PrefixedName
    case _ =>
      consume(7)                    // UnprefixedName
    }
    eventHandler.endNonterminal("Item", e0)
  }

  private def try_Whitespace: Unit = {
    l1 match {
    case 8 =>                       // S^WS
      consumeT(8)                   // S^WS
    case _ =>
      try_Comment
    }
  }

  private def try_Comment: Unit = {
    consumeT(11)                    // '(:'
    var c1 = true
    while (c1) {
      lookahead1(0)                 // CommentContents | '(:' | ':)'
      if (l1 == 14) {               // ':)'
        c1 = false
      }
      else {
        l1 match {
        case 9 =>                   // CommentContents
          consumeT(9)               // CommentContents
        case _ =>
          try_Comment
        }
      }
    }
    consumeT(14)                    // ':)'
  }

  def getErrorMessage(e: SequenceParser.ParseException) = {
    var message = e.getMessage
    val tokenSet = SequenceParser.getExpectedTokenSet(e)
    val found = SequenceParser.getOffendingToken(e)
    val size = e.end - e.begin
    message += (if (found == null) "" else ", found " + found) + "\nwhile expecting " +
      (if (tokenSet.length == 1) tokenSet(0) else "[" + (tokenSet mkString ", ") + "]") + "\n" +
      (if (size == 0 || found != null) "" else "after successfully scanning " + size + " characters beginning ")
    val prefix = input.substring(0, e.begin)
    val line = prefix.replaceAll("[^\n]", "").length + 1
    val column = prefix.length - prefix.lastIndexOf('\n')
    message +
      "at line " + line + ", column " + column + ":\n..." +
      input.substring(e.begin, math.min(input.length, e.begin + 64)) + "..."
  }

  private def consume(t: Int): Unit = {
    if (l1 == t) {
      whitespace
      eventHandler.terminal(SequenceParser.TOKEN(l1), b1, e1)
      b0 = b1; e0 = e1; l1 = 0
    }
    else {
      error(b1, e1, 0, l1, t)
    }
  }

  private def consumeT(t: Int): Unit = {
    if (l1 == t) {
      b0 = b1; e0 = e1; l1 = 0
    }
    else {
      error(b1, e1, 0, l1, t)
    }
  }

  private def skip(code: Int): Unit = {
    val b0W = b0; val e0W = e0

    l1 = code; b1 = begin; e1 = end

    try_Whitespace

    b0 = b0W; e0 = e0W
  }

  private def whitespace: Unit = {
    if (e0 != b1) {
      eventHandler.whitespace(e0, b1)
      e0 = b1
    }
  }

  private def matchW(tokenSetId: Int): Int = {
    var continue = true
    var code = 0
    while (continue) {
      code = matcher(tokenSetId)
      if (code != 8) {              // S^WS
        if (code != 11) {           // '(:'
          continue = false
        }
        else {
          skip(code)
        }
      }
    }
    code
  }

  private def lookahead1W(tokenSetId: Int): Unit = {
    if (l1 == 0) {
      l1 = matchW(tokenSetId)
      b1 = begin
      e1 = end
    }
  }

  private def lookahead1(tokenSetId: Int): Unit = {
    if (l1 == 0) {
      l1 = matcher(tokenSetId)
      b1 = begin
      e1 = end
    }
  }

  def error(b: Int, e: Int, s: Int, l: Int, t: Int): Int = {
    throw new SequenceParser.ParseException(b, e, s, l, t)
  }

  private def matcher(tokenSetId: Int) = {
    var nonbmp = false
    begin = end
    var current = end
    var result = SequenceParser.INITIAL(tokenSetId)
    var state = 0
    var code = result & 31

    while (code != 0) {
      var charclass = -1
      var c0 = if (current < size) input(current) else 0
      current += 1
      if (c0 < 0x80) {
        charclass = SequenceParser.MAP0(c0)
      }
      else if (c0 < 0xd800) {
        val c1 = c0 >> 4
        charclass = SequenceParser.MAP1((c0 & 15) + SequenceParser.MAP1((c1 & 31) + SequenceParser.MAP1(c1 >> 5)))
      }
      else {
        if (c0 < 0xdc00) {
          val c1 = if (current < size) input(current) else 0
          if (c1 >= 0xdc00 && c1 < 0xe000) {
            nonbmp = true
            current += 1
            c0 = ((c0 & 0x3ff) << 10) + (c1 & 0x3ff) + 0x10000
          }
        }

        var lo = 0
        var hi = 5
        var m = 3
        while (charclass < 0) {
          if (SequenceParser.MAP2(m) > c0) hi = m - 1
          else if (SequenceParser.MAP2(6 + m) < c0) lo = m + 1
          else charclass = SequenceParser.MAP2(12 + m)
          if (lo > hi) charclass = 0 else m = (hi + lo) >> 1
        }
      }

      state = code
      val i0 = (charclass << 5) + code - 1
      code = SequenceParser.TRANSITION((i0 & 3) + SequenceParser.TRANSITION(i0 >> 2))

      if (code > 31) {
        result = code
        code &= 31
        end = current
      }
    }

    result >>= 5
    if (result == 0) {
      end = current - 1
      val c1 = if (end < size) input(end) else 0
      if (c1 >= 0xdc00 && c1 < 0xe000) {
        end -= 1
      }
      error(begin, end, state, -1, -1)
    }
    else {
      if (nonbmp) {
        var i = result >> 4
        while (i > 0) {
          end -= 1
          val c1 = if (end < size) input(end) else 0
          if (c1 >= 0xdc00 && c1 < 0xe000) {
            end -= 1
          }
          i -= 1
        }
      }
      else {
        end -= result >> 4
      }
      if (end > size) end = size
      (result & 15) - 1
    }
  }

  var begin = 0
  var end = 0

  var b0 = 0
  var e0 = 0
  var l1 = 0
  var b1 = 0
  var e1 = 0
  var eventHandler: SequenceParser.EventHandler = null
  var input: String = null
  var size = 0
}

object SequenceParser {

  def getOffendingToken(e: ParseException) = {
    if (e.offending < 0) null else TOKEN(e.offending)
  }

  class ParseException(val begin: Int, val end: Int, val state: Int, val offending: Int, val expected: Int) extends RuntimeException {

    override def getMessage = {
      if (offending < 0) "lexical analysis failed" else "syntax error"
    }
  }

  def getExpectedTokenSet(e: ParseException) = {
    if (e.expected < 0) {
      getTokenSet(- e.state)
    }
    else {
      Array(TOKEN(e.expected))
    }
  }

  trait EventHandler {
    def reset(string: String): Unit
    def startNonterminal(name: String, begin: Int): Unit
    def endNonterminal(name: String, end: Int): Unit
    def terminal(name: String, begin: Int, end: Int): Unit
    def whitespace(begin: Int, end: Int): Unit
  }

  class TopDownTreeBuilder extends EventHandler {
    private var input: String = null
    private var stack = new ArrayBuffer[Nonterminal](64)
    private var top = -1

    override def reset(input: String): Unit = {
      this.input = input
      top = -1
    }

    override def startNonterminal(name: String, begin: Int): Unit = {
      val nonterminal = new Nonterminal(name, begin, begin, ArrayBuffer[Symbol]())
      if (top >= 0) addChild(nonterminal)
      top += 1
      if (top == stack.length) stack += nonterminal else stack(top) = nonterminal
    }

    override def endNonterminal(name: String, end: Int): Unit = {
      var nonterminal = stack(top)
      nonterminal.end = end
      if (top > 0) top -= 1
    }

    override def terminal(name: String, begin: Int, end: Int): Unit = {
      addChild(new Terminal(name, begin, end))
    }

    override def whitespace(begin: Int, end: Int): Unit = {
    }

    private def addChild(s: Symbol): Unit = {
      var current = stack(top)
      current.children += s
    }

    def serialize(e: EventHandler): Unit = {
      e.reset(input)
      stack(0).send(e)
    }
  }

  abstract class Symbol(n: String, b: Int, e: Int) {
    var name = n
    var begin = b
    var end = e

    def send(e: EventHandler): Unit
  }

  class Terminal(name: String, begin: Int, end: Int) extends Symbol(name, begin, end) {
    override def send(e: EventHandler): Unit = {
      e.terminal(name, begin, end)
    }
  }

  class Nonterminal(name: String, begin: Int, end: Int, c: ArrayBuffer[Symbol]) extends Symbol(name, begin, end) {
    var children = c

    override def send(e: EventHandler): Unit = {
      e.startNonterminal(name, begin)
      var pos = begin
      for (c <- children) {
        if (pos < c.begin) e.whitespace(pos, c.begin)
        c.send(e)
        pos = c.end
      }
      if (pos < end) e.whitespace(pos, end)
      e.endNonterminal(name, end)
    }
  }

  private def getTokenSet(tokenSetId: Int) = {
    var expected = ArrayBuffer[String]()
    val s = if (tokenSetId < 0) - tokenSetId else INITIAL(tokenSetId) & 31
    var i = 0
    while (i < 15) {
      var j = i
      val i0 = (i >> 5) * 29 + s - 1
      var f = EXPECTED(i0)
      while (f != 0) {
        if ((f & 1) != 0) {
          expected += TOKEN(j)
        }
        f >>>= 1
        j += 1
      }
      i += 32
    }
    expected.toArray
  }

  private final val MAP0 = Array(
    /*   0 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 18, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 1,
    /*  34 */ 2, 1, 1, 1, 1, 3, 4, 5, 1, 6, 7, 8, 9, 1, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 1, 1, 1, 1, 1, 1,
    /*  65 */ 12, 12, 12, 12, 13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 14, 12, 12, 12, 12, 12, 12, 12, 12, 12, 1,
    /*  92 */ 1, 1, 1, 12, 1, 12, 12, 12, 12, 13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    /* 119 */ 12, 12, 12, 12, 15, 1, 16, 1, 1
  )

  private final val MAP1 = Array(
    /*   0 */ 108, 124, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 156, 181, 181, 181, 181,
    /*  21 */ 181, 214, 215, 213, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  42 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  63 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  84 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /* 105 */ 214, 214, 214, 254, 247, 270, 286, 327, 313, 327, 349, 371, 371, 371, 363, 396, 388, 396, 388, 396, 396,
    /* 126 */ 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 412, 412, 412, 412, 412, 412, 412,
    /* 147 */ 333, 396, 396, 396, 396, 396, 396, 396, 396, 297, 371, 371, 372, 370, 371, 371, 396, 396, 396, 396, 396,
    /* 168 */ 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 371, 371, 371, 371, 371, 371, 371, 371,
    /* 189 */ 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371, 371,
    /* 210 */ 371, 371, 371, 395, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396,
    /* 231 */ 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 396, 371, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    /* 256 */ 0, 0, 0, 0, 0, 0, 0, 18, 18, 0, 0, 18, 0, 0, 18, 1, 2, 1, 1, 1, 1, 3, 4, 5, 1, 6, 7, 8, 9, 1, 10, 10, 10,
    /* 289 */ 10, 10, 10, 10, 10, 10, 10, 11, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 12, 12, 1, 1, 12, 14, 12, 12, 12, 12,
    /* 319 */ 12, 12, 12, 12, 12, 1, 1, 1, 1, 12, 12, 12, 12, 13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    /* 346 */ 12, 1, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 15, 1, 16, 1, 1, 1, 1, 1, 1, 1, 17, 1, 1, 1, 1, 1,
    /* 376 */ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 17, 12, 12, 12, 12, 12, 12, 12, 1, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    /* 405 */ 12, 12, 12, 12, 12, 12, 12, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17
  )

  private final val MAP2 = Array(
    /*  0 */ 57344, 63744, 64976, 65008, 65536, 983040, 63743, 64975, 65007, 65533, 983039, 1114111, 1, 12, 1, 12, 12,
    /* 17 */ 1
  )

  private final val INITIAL = Array(
    /* 0 */ 1, 2, 3, 4, 5
  )

  private final val TRANSITION = Array(
    /*   0 */ 173, 173, 173, 173, 173, 173, 173, 173, 172, 167, 186, 173, 170, 173, 152, 173, 258, 261, 244, 173, 264,
    /*  21 */ 173, 152, 173, 181, 255, 298, 173, 170, 184, 152, 173, 157, 161, 186, 173, 164, 173, 152, 173, 296, 178,
    /*  42 */ 186, 173, 190, 173, 152, 173, 172, 167, 186, 173, 170, 280, 152, 173, 197, 167, 186, 173, 170, 173, 152,
    /*  63 */ 173, 172, 167, 186, 304, 170, 280, 203, 278, 212, 215, 186, 238, 170, 173, 203, 278, 218, 221, 186, 228,
    /*  84 */ 170, 235, 232, 278, 242, 248, 252, 199, 268, 173, 152, 173, 224, 209, 186, 304, 170, 153, 275, 278, 224,
    /* 105 */ 209, 186, 206, 170, 193, 275, 278, 284, 287, 186, 304, 170, 153, 275, 278, 172, 167, 186, 174, 170, 173,
    /* 126 */ 173, 173, 172, 167, 186, 173, 170, 173, 302, 173, 172, 167, 186, 304, 170, 173, 203, 278, 290, 293, 271,
    /* 147 */ 173, 170, 173, 152, 173, 25, 0, 0, 0, 251, 6, 9, 9, 361, 9, 17, 17, 17, 0, 17, 0, 8, 8, 8, 0, 8, 0, 0, 0,
    /* 176 */ 0, 25, 416, 8, 480, 8, 0, 12, 12, 0, 0, 0, 11, 12, 8, 1344, 1344, 0, 23, 0, 251, 8, 448, 0, 0, 24, 24, 25,
    /* 204 */ 0, 251, 0, 23, 271, 271, 8, 8, 8, 0, 13, 13, 8, 8, 8, 0, 78, 78, 8, 8, 8, 0, 271, 271, 118, 78, 271, 271,
    /* 232 */ 25, 154, 251, 0, 118, 154, 0, 118, 271, 271, 7, 0, 0, 0, 180, 12, 0, 402, 19, 19, 384, 0, 11, 12, 8, 8, 8,
    /* 259 */ 0, 11, 11, 8, 8, 8, 0, 8, 11, 1362, 0, 19, 0, 298, 11, 12, 25, 0, 251, 221, 0, 0, 0, 26, 0, 8, 0, 272,
    /* 287 */ 272, 8, 8, 8, 298, 298, 298, 8, 8, 8, 416, 0, 0, 11, 181, 28, 0, 0, 0, 271, 271
  )

  private final val EXPECTED = Array(
    /*  0 */ 18944, 14592, 2558, 3582, 6654, 2560, 16896, 512, 2048, 256, 16, 16, 12, 14, 192, 224, 512, 512, 512, 16,
    /* 20 */ 16, 12, 8, 64, 32, 8, 64, 32, 32
  )

  private final val TOKEN = Array(
    "(0)",
    "IntegerLiteral",
    "DecimalLiteral",
    "DoubleLiteral",
    "StringLiteral",
    "URIQualifiedName",
    "PrefixedName",
    "UnprefixedName",
    "S",
    "CommentContents",
    "'('",
    "'(:'",
    "')'",
    "','",
    "':)'"
  )
}

// End
