package com.xmlcalabash.model.util

trait ExpressionParser {
  def parse(expr: String): Unit
  def variableRefs: List[String]
  def functionRefs: List[String]
}
