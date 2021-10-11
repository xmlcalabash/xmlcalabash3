package com.xmlcalabash.model.util

import com.jafpl.graph.Location
import com.jafpl.steps.Step
import com.jafpl.util.ErrorListener
import com.xmlcalabash.config.Signatures
import net.sf.saxon.s9api.QName

trait ParserConfiguration {
  def errorListener: ErrorListener
  def stepSignatures: Signatures
  def stepImplementation(name: QName, location: Location): Step

  /** Enable trace events.
    *
    * The actors that run steps will emit log messages if the appropriate traces are enabled.
    * This method is called to determine if a particular trace is enabled.
    *
    * @param trace A trace event.
    * @return True, if that event should be considered enabled.
    */
  def traceEnabled(trace: String): Boolean
  def expressionParser: ExpressionParser
}
