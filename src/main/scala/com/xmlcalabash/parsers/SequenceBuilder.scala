package com.xmlcalabash.parsers

import com.xmlcalabash.exceptions.ParseException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.parsers.SequenceParser.EventHandler
import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

class TypedSequenceItem(val item: String, val as: QName) {
  // nop
}

class SequenceBuilder() extends EventHandler {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var input: String = _
  private val items = ListBuffer.empty[TypedSequenceItem]

  def parse(in: String): List[TypedSequenceItem] = {
    input = null
    val parser = new SequenceParser(in, this)

    try {
      parser.parse_Sequence
    } catch {
      case err: SequenceParser.ParseException =>
        throw new ParseException("Syntax error", None)
    }

    items.toList
  }

  def reset(string: String): Unit = {
    input = string
  }

  def startNonterminal(name: String, begin: Int): Unit = {
    //println(" nt " + name)

    name match {
      case "Sequence" => ()
      case "Item" => ()
      case _ => println(s"Unexpected NT: $name")
    }
  }

  def endNonterminal(name: String, end: Int): Unit = {
    //println("/nt " + name)

    name match {
      case "Sequence" => ()
      case "Item" => ()
      case _ => println("Unexpected /NT: " + name)
    }
  }

  def terminal(name: String, begin: Int, end: Int): Unit = {
    val tag = if (name(0) == '\'') "TOKEN" else name
    val text = characters(begin, end)

    //println(tag + ": " + text)

    if (tag == "TOKEN") {
      text match {
        case "(" => ()
        case "," => ()
        case ")" => ()
        case _ => println(s"Unexpected token: $text")
      }
    } else {
      name match {
        case "StringLiteral" =>
          val str = text.substring(1, text.length - 1)
          items += new TypedSequenceItem(str, XProcConstants.xs_string)
        case "IntegerLiteral" => items += new TypedSequenceItem(text, XProcConstants.xs_integer)
        case "DecimalLiteral" => items += new TypedSequenceItem(text, XProcConstants.xs_decimal)
        case "DoubleLiteral" => items += new TypedSequenceItem(text, XProcConstants.xs_double)
        case "URIQualifiedName" => new TypedSequenceItem(text, XProcConstants.xs_QName)
        case "PrefixedName" => items += new TypedSequenceItem(text, XProcConstants.xs_QName)
        case "UnprefixedName" => items += new TypedSequenceItem(text, XProcConstants.xs_NCName)
        case _ => println("Unexpected T: " + name + ": " + text)
      }
    }
  }

  def whitespace(begin: Int, end: Int): Unit = {
    // nop
  }

  private def characters(begin: Int, end: Int): String = {
    if (begin < end) {
      input.substring(begin, end)
    } else {
      ""
    }
  }
}
