package com.xmlcalabash.util

import com.jafpl.messages.Message
import com.jafpl.util.ItemTester
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.xml.ForWhile
import com.xmlcalabash.runtime.{DynamicContext, StaticContext, XProcXPathExpression}

class XmlItemTester(config: XMLCalabashConfig, comparator: String, maxIterations: Long, art: ForWhile) extends ItemTester {
  private var count = 0L

  override def test(item: List[Message], bindings: Map[String, Message]): Boolean = {
    if (maxIterations >= 0 && count >= maxIterations) {
      throw XProcException.xiMaxRegressionsExceededWhile(maxIterations, None)
    }
    count += 1

    val context = new StaticContext(config)
    val expr = new XProcXPathExpression(context, comparator)

    var pass = false
    val dynamicContext = new DynamicContext(Some(art))
    DynamicContext.withContext(dynamicContext) {
      val exeval = config.expressionEvaluator.newInstance()
      pass = exeval.booleanValue(expr, item, bindings, None)
    }

    pass
  }
}
