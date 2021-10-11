package com.xmlcalabash.steps.text

import com.jafpl.messages.Message
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmNodeItemMessage
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.{QName, XdmNode}

class Replace() extends DefaultXmlStep {
  private val _pattern = new QName("", "pattern")
  private val _replacement = new QName("", "replacement")
  private val _flags = new QName("", "flags")
  private var text: XdmNode = _
  private var meta: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode => text = node
      case _ =>
        throw XProcException.xiUnexpectedItem(item.toString, location)
    }
    meta = metadata
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val pattern = stringBinding(_pattern).replace("'", "''")
    val replacement = stringBinding(_replacement).replace("'", "''")
    val flags = optionalStringBinding(_flags)

    val replexpr = if (flags.isDefined) {
      s"replace(., '$pattern', '$replacement', '${flags.get}')"
    } else {
      s"replace(., '$pattern', '$replacement')"
    }
    val expr = new XProcXPathExpression(context, replexpr)
    val contextItem = new XdmNodeItemMessage(text, meta, context)

    val evaluator = config.expressionEvaluator.newInstance()
    val repl = evaluator.singletonValue(expr, List(contextItem), Map.empty[String,Message], None)

    consumer.get.receive("result", repl.item, meta)
  }

}
