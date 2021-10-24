package com.xmlcalabash.util

import com.jafpl.messages.Message
import com.jafpl.util.ItemTester
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.xxml.{XForWhile, XStaticContext}
import com.xmlcalabash.runtime.{DynamicContext, SaxonExpressionEvaluator, XMLCalabashRuntime, XProcXPathExpression}

class XmlItemTester(runtime: XMLCalabashRuntime, comparator: String, maxIterations: Long, art: XForWhile) extends ItemTester {
  private var count = 0L

  override def test(item: List[Message], bindings: Map[String, Message]): Boolean = {
    if (maxIterations >= 0 && count >= maxIterations) {
      throw XProcException.xiMaxRegressionsExceededWhile(maxIterations, None)
    }
    count += 1

    val expr = new XProcXPathExpression(art.staticContext, comparator)

    var pass = false
    val dynamicContext = new DynamicContext(runtime, art)
    DynamicContext.withContext(dynamicContext) {
      val exeval = runtime.expressionEvaluator.newInstance()
      pass = exeval.booleanValue(expr, item, bindings, None)
    }

    pass
  }
}
