package com.xmlcalabash.model.util

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants.ValueTemplate
import com.xmlcalabash.util.{MinimalStaticContext, ValueTemplateParser}
import net.sf.saxon.expr.parser.ExpressionTool
import net.sf.saxon.s9api.{QName, SaxonApiException}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object XValueParser {
  private val contextFunctions =
    List(XProcConstants.p_iteration_size, XProcConstants.p_iteration_position, XProcConstants.fn_collection)
  private val nonStaticFunctions =
    List(XProcConstants.fn_current_dateTime, XProcConstants.fn_current_date,
      XProcConstants.fn_current_time, XProcConstants.fn_implicit_timezone,
      XProcConstants.fn_default_collation, XProcConstants.fn_default_language,
      XProcConstants.fn_static_base_uri, XProcConstants.p_system_property
      // I don't think position() and last() are non-static from XProc's perspective...
    )

  def parseAvt(value: String): ValueTemplate = {
    val parser = new ValueTemplateParser(value)
    parser.template()
  }
}

class XValueParser private(config: XMLCalabash, context: MinimalStaticContext) {
  private var _contextDependent = false
  private var _static = true
  private val _variableRefs = mutable.HashSet.empty[QName]
  private val _functionRefs = mutable.HashSet.empty[QName]
  private val _expressions = ListBuffer.empty[String]
  private val _segments = ListBuffer.empty[ExpressionParser]

  def contextDependent: Boolean = _contextDependent
  def static: Boolean = _static && !_contextDependent
  def variables: Set[QName] = _variableRefs.toSet
  def functions: Set[QName] = _functionRefs.toSet

  def this(config: XMLCalabash, context: MinimalStaticContext, avt: ValueTemplate) = {
    this(config, context)
    var inexpr = false
    for (substr <- avt) {
      if (inexpr) {
        _expressions += substr

        val parser = config.expressionParser
        parser.parse(substr)
        _segments += parser
      }
      inexpr = !inexpr
    }

    findVariableRefs()
    findFunctionRefs()
    dependsOnContext()
  }

  def this(config: XMLCalabash, context: MinimalStaticContext, select: String) = {
    this(config, context)
    _expressions += select

    val parser = config.expressionParser
    parser.parse(select)
    _segments += parser

    findVariableRefs()
    findFunctionRefs()
    dependsOnContext()
  }

  private def findVariableRefs(): Unit = {
    for (segment <- _segments) {
      for (ref <- segment.variableRefs) {
        _variableRefs += context.parseClarkName(ref)
      }
    }
  }

  private def findFunctionRefs(): Unit = {
    for (segment <- _segments) {
      for (ref <- segment.functionRefs) {
        val qname = context.parseQName(ref)
        if (Option(qname.getNamespaceURI).isEmpty || qname.getNamespaceURI == "") {
          _functionRefs += new QName("fn", XProcConstants.ns_fn, qname.getLocalName)
        } else {
          _functionRefs += qname
        }
      }
    }
  }

  private def dependsOnContext(): Unit = {
    // Make sure functions and variables have been computed first!
    for (func <- _functionRefs) {
      _contextDependent = _contextDependent || XValueParser.contextFunctions.contains(func)
      if (XValueParser.nonStaticFunctions.contains(func)) {
        _static = false
      }
    }

    if (!_contextDependent) {
      for (expr <- _expressions) {
        val xcomp = config.processor.newXPathCompiler()
        for ((prefix, uri) <- context.inscopeNamespaces) {
          xcomp.declareNamespace(prefix, uri)
        }
        for (name <- _variableRefs) {
          xcomp.declareVariable(name)
        }
        try {
          val xexec = xcomp.compile(expr)
          val xexpr = xexec.getUnderlyingExpression.getInternalExpression
          _contextDependent = _contextDependent || ExpressionTool.dependsOnFocus(xexpr)
        } catch {
              /*
          case ex: SaxonApiException =>
            throw XProcException.xsStaticErrorInExpression(expr, ex.getMessage, None)
          case ex: Exception =>
            throw ex
               */
          case ex: Exception => _static = false
        }
      }
    }
  }
}
