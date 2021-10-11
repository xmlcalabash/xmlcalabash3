package com.xmlcalabash.parsers

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.ExpressionParser
import com.xmlcalabash.parsers.XPath31.EventHandler
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.HashSet
import scala.collection.mutable

class XPathParser() extends ExpressionParser {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val handler = new FindRefs()
  private val parser = new XPath31()
  private var _errors = false
  private var _trace = false

  def this(cfg: XMLCalabashConfig) = {
    this()
    _trace = cfg.traceEventManager.traceEnabled("XPathParser")
  }

  def trace: Boolean = _trace
  def trace_=(t: Boolean): Unit = {
    _trace = t
  }

  def parse(expr: String): Unit = {
    handler.initialize()
    parser.initialize(expr, handler)

    if (trace) {
      logger.debug("XPathParser:  parse: {}", expr)
    }

    try {
      parser.parse_XPath
    } catch {
      case _: Throwable =>
        _errors = true
    }
  }

  def errors: Boolean = _errors

  def variableRefs: List[String] = {
    handler.variableRefs()
  }

  def functionRefs: List[String] = {
    handler.functionRefs()
  }

  def contextRef: Boolean = handler.contextRef

  class FindRefs extends EventHandler {
    private var input: String = _
    private val varlist = mutable.ListBuffer.empty[String]
    private val funclist = mutable.ListBuffer.empty[String]
    private var sawDollar = false
    // Simple switches won't work if they can nest, but I don't think they can...
    private var functionCall = false
    private var functionName = false
    private var context = false
    private var quantified = false // I bet this one can nest...
    private var quantvar = HashSet.empty[String]

    def initialize(): Unit = {
      input = null
      varlist.clear()
      funclist.clear()
      sawDollar = false
      functionCall = false
      functionName = false
    }

    def variableRefs(): List[String] = {
      varlist.toList
    }

    def functionRefs(): List[String] = {
      funclist.toList
    }

    def contextRef: Boolean = context

    override def reset(string: String): Unit = {
      input = string
    }

    override def startNonterminal(name: String, begin: Int): Unit = {
      if (trace) {
        logger.debug("XPathParser:  NT: {}", name)
      }
      name match {
        case "PathExpr" => context = true
        case "FunctionCall" => functionCall = true
        case "FunctionName" => functionName = true
        case "QuantifiedExpr" => quantified = true
        case _ => ()
      }
    }

    override def endNonterminal(name: String, end: Int): Unit = {
      if (trace) {
        logger.debug("XPathParser: /NT: {}", name)
      }
      name match {
        case "FunctionCall" => functionCall = false
        case "FunctionName" => functionName = false
        case _ => ()
      }
    }

    override def terminal(name: String, begin: Int, end: Int): Unit = {
      if (trace) {
        logger.debug(s"XPathParser:   T: $name: ${characters(begin,end)}")
      }
      if (sawDollar) {
        val varname = characters(begin, end)
        if (quantified) {
          quantvar += varname
        } else {
          if (!quantvar.contains(varname)) {
            varlist += varname
          }
        }
      } else {
        if (functionCall && functionName) {
          funclist += characters(begin, end)
        }
      }
      sawDollar = name == "'$'"
      if (quantified && name == "in") {
        quantified = false
      }
    }

    override def whitespace(begin: Int, end: Int): Unit = {
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
}
