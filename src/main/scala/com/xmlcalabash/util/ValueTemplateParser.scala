package com.xmlcalabash.util

import com.xmlcalabash.model.util.XProcConstants.ValueTemplate

import scala.collection.mutable.ListBuffer

class ValueTemplateParser(expr: String) {
  /*
  This class parses value templates. Value templates have the from "string{expr}s{{t}}ring{expr}string" etc.
  The "string" portions can be empty. The tricky bit is that the "expr" portions are XPath expressions
  and can contain { and }. Less tricky, is that outside an expr, {{ represents { and }} represents }.
  So, the example above is "string", "expr", "s{t}ring", "expr", "string".

  Inside an expr, { and } are ignored inside strings or comments. An unbalanced } ends the expression.
  (I don't think a balanced pair of { } is valid in XPath 3.1, but let's look towards the future.
   */
  private val DOUBLEQUOTE = "\""
  private val SINGLEQUOTE ="'"
  private var pos = 0;

  def template(): ValueTemplate = {
    val template = ListBuffer.empty[String]

    while (more) {
      val str = parseString
      template += str
      if (more) {
        val str = parseExpr
        template += str
      }
    }

    template.toList
  }

  private def parseString: String = {
    val acc = new StringBuilder()
    var moreString = more
    while (moreString) {
      current match {
        case "{" =>
          if (peek("{")) {
            pos += 1
            acc.append(current)
            pos += 1
            moreString = more
          } else {
            pos += 1
            moreString = false
            if (current == "") {
              throw new RuntimeException("Unmatched { appears at end of string")
            }
          }
        case "}" =>
          if (peek("}")) {
            pos += 1
            acc.append(current)
            pos += 1
            moreString = more
          } else {
            throw new RuntimeException("Unescaped } in string")
          }
        case _ =>
          acc.append(current)
          pos += 1
          moreString = more
      }
    }

    acc.toString()
  }

  private def parseExpr: String = {
    val acc = new StringBuilder()
    var moreString = more
    var commentDepth = 0
    var braceDepth = 1
    var state = State.ORDINARY

    while (moreString) {
      state match {
        case State.ORDINARY =>
          current match {
            case "(" =>
              if (peek(":")) {
                state = State.COMMENT
                commentDepth += 1
              }
              acc.append(current)
              pos += 1
              moreString = more
            case "\"" =>
              state = State.DSTRING
              acc.append(current)
              pos += 1
              moreString = more
            case "'" =>
              state = State.SSTRING
              acc.append(current)
              pos += 1
              moreString = more
            case "{" =>
              braceDepth += 1
              acc.append(current)
              pos += 1
              moreString = more
            case "}" =>
              braceDepth -= 1
              if (braceDepth > 0) {
                acc.append(current)
                moreString = more
              } else {
                moreString = false
              }
              pos += 1
            case _ =>
              acc.append(current)
              pos += 1
              moreString = more
          }

        case State.SSTRING | State.DSTRING =>
          current match {
            case DOUBLEQUOTE =>
              if (state == State.DSTRING) {
                if (peek(DOUBLEQUOTE)) {
                  acc.append("\"\"")
                  pos += 2
                } else {
                  acc.append(current)
                  pos += 1
                  state = State.ORDINARY
                }
              } else {
                acc.append(current)
                pos += 1
              }
              moreString = more
            case SINGLEQUOTE =>
              if (state == State.SSTRING) {
                if (peek(SINGLEQUOTE)) {
                  acc.append("''")
                  pos += 2
                } else {
                  acc.append(current)
                  pos += 1
                  state = State.ORDINARY
                }
              } else {
                acc.append(current)
                pos += 1
              }
              moreString = more
            case _ =>
              acc.append(current)
              pos += 1
              moreString = more
          }

        case State.COMMENT =>
          current match {
            case ":" =>
              if (peek(")")) {
                commentDepth -= 1
                acc.append(":)")
                pos += 2
                if (commentDepth == 0) {
                  state = State.ORDINARY
                }
              } else {
                acc.append(current)
                pos += 1
              }
              moreString = more
            case _ =>
              acc.append(current)
              pos += 1
              moreString = more
          }
      }
    }

    if (state != State.ORDINARY) {
      throw new RuntimeException("Invalid AVT (in quoted string or coment at end of expression)")
    }

    if (state != State.ORDINARY) {
      throw new RuntimeException("Invalid AVT")
    }

    if (braceDepth != 0) {
      throw new RuntimeException("Invalid AVT, unmatched {")
    }

    acc.toString()
  }

  private def more: Boolean = {
    pos < expr.length
  }

  private def current: String = {
    if (pos < expr.length) {
      expr.substring(pos,pos+1)
    } else {
      ""
    }
  }

  private def peek(next: String): Boolean = {
    if (pos+1 < expr.length) {
      next == expr.substring(pos+1,pos+2)
    } else {
      false
    }
  }
  private object State {
    val ORDINARY = 0
    val COMMENT = 1
    val SSTRING = 2
    val DSTRING = 3
  }

}
