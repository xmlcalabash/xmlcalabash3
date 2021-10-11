package com.xmlcalabash.util

import com.xmlcalabash.config.ErrorExplanation
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

class DefaultErrorExplanation() extends ErrorExplanation {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val messages = ListBuffer.empty[ErrorExplanationTemplate]

  private var namespaces = mutable.HashMap.empty[String, String]
  private var code = Option.empty[String]
  private var variant = 1
  private var message = ""
  private var explanation = ""
  private val stream = getClass.getResourceAsStream("/xproc-errors.txt")

  namespaces.put("", "http://xmlcalabash.com/ns/ERROR")

  for (line <- Source.fromInputStream(stream, "UTF-8").getLines()) {
    if (line == "") {
      if (code.isDefined) {
        messages += new ErrorExplanationTemplate(code.get, variant, message, explanation)
        code = None
        variant = 1
        message = ""
        explanation = ""
      }
    } else {
      val Namespace = "namespace\\s+(\\S+)\\s*=\\s*(.*)\\s*".r
      line match {
        case Namespace(prefix, uri) =>
          namespaces.put(prefix, uri)
        case _ =>
          if (code.isEmpty) {
            val PrefixCode = "([^{]\\S*):(\\S+)".r
            val PrefixCodeVar = "([^{]\\S*):(\\S+)\\s*/\\s*(\\d+)".r
            val BareCode = "([^:\\s]+)".r
            val BareCodeVar = "([^:\\s]+)\\s*/\\s*(\\d+)".r
            val ClarkCode = "\\{(.*)\\}\\s*(\\S+)".r
            val ClarkCodeVar = "\\{(.*)\\}\\s*(\\S+)\\s*/\\s*(\\d+)".r

            val qcode = line match {
              case PrefixCodeVar(prefix, localname, vcode) =>
                variant = vcode.toInt
                var uri = namespaces.getOrElse(prefix, "")
                if (uri == "") {
                  logger.error(s"Invalid error code: ${prefix}:${localname}, no namespace binding for ${prefix} in error explanations.")
                  uri = namespaces("")
                }
                Some(new QName(uri, localname))
              case PrefixCode(prefix, localname) =>
                variant = 1
                var uri = namespaces.getOrElse(prefix, "")
                if (uri == "") {
                  logger.error(s"Invalid error code: ${prefix}:${localname}, no namespace binding for ${prefix} in error explanations.")
                  uri = namespaces("")
                }
                Some(new QName(uri, localname))
              case ClarkCodeVar(namespace, localname, vcode) =>
                variant = vcode.toInt
                Some(new QName(namespace, localname))
              case ClarkCode(namespace, localname) =>
                variant = 1
                Some(new QName(namespace, localname))
              case BareCodeVar(bcode, vcode) =>
                variant = vcode.toInt
                Some(new QName(XProcConstants.ns_err, bcode))
              case BareCode(bcode) =>
                variant = 1
                Some(new QName(XProcConstants.ns_err, bcode))
              case _  =>
                logger.info(s"Expected error code on line: $line")
                None
            }

            if (qcode.isDefined) {
              code = Some(qcode.get.getClarkName)
            } else {
              // If it's *still* empty, move along
              if (message == "") {
                message = line
              } else {
                explanation += line + "\n"
              }
            }
          } else if (message == "") {
            message = line
          } else {
            explanation += line + "\n"
          }
      }
    }
  }

  if (code.isDefined) {
    messages += new ErrorExplanationTemplate(code.get, variant, message, explanation)
  }

  override def message(code: QName, variant: Int): String = {
    message(code, variant, List.empty[Any])
  }

  override def message(code: QName, variant: Int, details: List[Any]): String = {
    var message = template(code, variant, details.length).message
    substitute(message, details)
  }

  override def explanation(code: QName, variant: Int): String = {
    explanation(code, variant, List.empty[Any])
  }

  override def explanation(code: QName, variant: Int, details: List[Any]): String = {
    var message = template(code, variant, details.length).explanation
    substitute(message, details)
  }

  private def template(code: QName, variant: Int, count: Integer): ErrorExplanationTemplate = {
    val clark = code.getClarkName
    // Find all the messages with a matching code and variant, with a cardinality <= details.length
    val a1 = messages.filter(_.code == clark)
    val a2 = a1.filter(_.variant == variant)
    val a3 = a2.filter(_.cardinality <= count)

    val templates = messages.filter(_.code == clark).filter(_.variant == variant).filter(_.cardinality <= count)

    if (templates.isEmpty) {
      // Return a default template
      new ErrorExplanationTemplate(code.getClarkName, 1,"[No explanatory message for " + code + "]", "")
    } else {
      // Return the (first) one with the matching cardinality
      for (template <- templates) {
        if (template.cardinality == count) {
          return template
        }
      }

      // Return the one with the highest cardinality.
      templates.maxBy(_.cardinality)
    }
  }

  private def substitute(text: String, details: List[Any]): String = {
    var message = text
    val detail = "^(.*?)\\$(\\d+)(.*)$".r
    var matched = true

    while (matched) {
      matched = false
      message match {
        case detail(pre,detno,post) =>
          matched = true
          val detnum = detno.toInt - 1
          if (details.length > detnum) {
            message = pre + stringify(details(detnum)) + post
          } else {
            message = pre + post
          }
        case _ =>
      }
    }

    message
  }

  private def stringify(any: Any): String = {
    any match {
      case list: List[Any] =>
        var str = "["
        var sep = ""
        for (item <- list) {
          str = str + sep + item.toString
          sep = ", "
        }
        str = str + "]"
        str
      case _ => any.toString
    }
  }

  private class ErrorExplanationTemplate(val code: String, val variant: Int, val message: String, val explanation: String) {
    def cardinality: Int = message.count(_=='$')
  }

}
