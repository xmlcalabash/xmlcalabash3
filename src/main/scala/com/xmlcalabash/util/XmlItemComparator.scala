package com.xmlcalabash.util

import com.jafpl.messages.Message
import com.jafpl.util.ItemComparator
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.xxml.{XForUntil, XStaticContext}
import com.xmlcalabash.runtime.{DynamicContext, StaticContext, XProcMetadata, XProcXPathExpression}
import net.sf.saxon.s9api.{XdmNode, XdmValue}

import scala.collection.mutable

class XmlItemComparator(config: XMLCalabash, comparator: String, maxIterations: Long, art: XForUntil) extends ItemComparator {
  private var count = 0L

  override def areTheSame(a: Any, b: Any): Boolean = {
    if (maxIterations >= 0 && count >= maxIterations) {
      throw XProcException.xiMaxRegressionsExceededUntil(maxIterations, None)
    }
    count += 1

    a match {
      case anode: XdmNode =>
        b match {
          case bnode: XdmNode =>
            sameNode(anode, bnode)
          case _ => false
        }
      case avalue: XdmValue =>
        b match {
          case bvalue: XdmValue =>
            sameValue(avalue, bvalue)
          case _ => false
        }
      case _ => a == b
    }
  }

  private def sameNode(anode: XdmNode, bnode: XdmNode): Boolean = {
    val context = new XStaticContext()
    val expr = new XProcXPathExpression(context, comparator)
    val bindingsMap = mutable.HashMap.empty[String, Message]
    val amsg = new XdmValueItemMessage(anode, XProcMetadata.XML, context)
    bindingsMap.put("{}a", amsg)
    val bmsg = new XdmValueItemMessage(bnode, XProcMetadata.XML, context)
    bindingsMap.put("{}b", bmsg)

    var same = false
    val dynamicContext = new DynamicContext(art)
    DynamicContext.withContext(dynamicContext) {
      val exeval = config.expressionEvaluator.newInstance()
      same = exeval.booleanValue(expr, List(amsg), bindingsMap.toMap, None)
    }

    same
  }

  private def sameValue(avalue: XdmValue, bvalue: XdmValue): Boolean = {
    avalue.equals(bvalue)
  }
}
