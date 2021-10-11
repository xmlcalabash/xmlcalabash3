package com.xmlcalabash.functions

import com.jafpl.messages.Message
import com.xmlcalabash.runtime.SaxonExpressionEvaluator
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.om.{Item, NodeInfo}
import org.slf4j.{Logger, LoggerFactory}

abstract class FunctionImpl extends ExtensionFunctionDefinition {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected def getMessage(item: Item, exprEval: SaxonExpressionEvaluator): Option[Message] = {
    // Walk up the tree if we get passed some descendant
    var arg = item
    val msg = exprEval.dynContext.get.message(arg)

    if (msg.isDefined) {
      msg
    } else {
      var doc: NodeInfo = null
      var done = false

      while (!done) {
        arg match {
          case node: NodeInfo =>
            if (node.getParent == null) {
              doc = node
              done = true
            } else {
              arg = node.getParent
            }
          case _ =>
            done = true
        }
      }

      if (doc != null) {
        exprEval.dynContext.get.message(doc)
      } else {
        None
      }
    }
  }
}
