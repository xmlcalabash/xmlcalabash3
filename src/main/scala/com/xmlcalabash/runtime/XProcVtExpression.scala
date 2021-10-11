package com.xmlcalabash.runtime

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.ValueParser
import net.sf.saxon.expr.parser.ExpressionTool

class XProcVtExpression private(override val context: StaticContext, val params: Option[ExprParams]) extends XProcExpression(context) {
  private var _avt: List[String] = _
  private var _string = false

  def this(context: StaticContext, avt: List[String], stringResult: Boolean) = {
    this(context, None)
    _avt = avt
    _string = stringResult
  }

  def this(context: StaticContext, avt: List[String]) = {
    this(context, avt, false)
  }

  def this(context: StaticContext, expr: String, stringResult: Boolean) = {
    this(context, None)
    val avt = ValueParser.parseAvt(expr)
    if (avt.isEmpty) {
      throw XProcException.xiInvalidAVT(context.location, expr)
    }
    _avt = avt.get
    _string = stringResult
  }

  def this(context: StaticContext, expr: String) = {
    this(context, expr, false)
  }

  def avt: List[String] = _avt
  def stringResult: Boolean = _string

  override def toString: String = {
    var str = ""
    var isavt = false
    for (item <- avt) {
      if (isavt) {
        str += "{" + item + "}"
      } else {
        str += item
      }
      isavt = !isavt
    }
    str
  }

  def checkContext(config: XMLCalabashRuntime): Unit = {
    var isexpr = false
    for (xpathexpr <- avt) {
      if (isexpr) {
        val compiler = config.processor.newXPathCompiler()
        val expr = compiler.compile(xpathexpr).getUnderlyingExpression.getInternalExpression
        val focus = ExpressionTool.dependsOnFocus(expr)
        //print(focus,":",xpathexpr)
      }
      isexpr = !isexpr
    }
  }
}
