package com.xmlcalabash.model.xxml

import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{XProcConstants, XValueParser}
import com.xmlcalabash.runtime.{XProcVtExpression, XProcXPathExpression}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XWhen(config: XMLCalabash) extends XChooseBranch(config) {
  private val _namebindings = ListBuffer.empty[XNameBinding]
  private var _constantValue = Option.empty[XdmValueItemMessage]
  private var _contextDependent = false
  private var _wasIf = false

  def this(choose: XChoose, test: String, collection: Boolean) = {
    this(choose.config)
    staticContext = choose.staticContext
    parent = choose
    synthetic = true
    syntheticName = XProcConstants.p_when
    _test = test
    _collection = collection
  }

  def this(choose: XChoose, test: String, collection: Boolean, wasIf: Boolean) = {
    this(choose, test, collection)
    _wasIf = wasIf
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    val coll = attr(XProcConstants._collection)
    if (coll.isDefined) {
      coll.get match {
        case "true" => _collection = true
        case "false" => _collection = false
        case _ =>
          error(XProcException.xsBadTypeValue(coll.get, "xs:boolean", None))
      }
    }

    if (attributes.contains(XProcConstants._test)) {
      _test = attr(XProcConstants._test).get
    } else {
      error(XProcException.xsMissingRequiredAttribute(XProcConstants._test, None))
    }
  }

  override def validate(): Unit = {
    if (synthetic) {
      // There are no attributes and we've already validated the children
      return
    }
    checkAttributes()
    checkEmptyAttributes()

    var seenWithInput = false
    var seenPipeline = false

    //val newScope = checkStepNameScoping(inScopeNames)
    for (child <- allChildren) {
      child.validate()
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _: XWithInput =>
          if (seenWithInput) {
            error(XProcException.xiUserError("More than one p:with-input in choose"))
          }
          if (seenPipeline) {
            error(XProcException.xiUserError("with-input can't follow steps"))
          }
          seenWithInput = true
        case _: XOutput =>
          if (seenPipeline) {
            error(XProcException.xiUserError("output can't follow steps"))
          }
        case _: XVariable =>
          seenPipeline = true
        case _: XStep =>
          seenPipeline = true
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    if (children[XOutput].length == 1) {
      val output = children[XOutput].head
      if (!output.primarySpecified) {
        output.primary = true
      }
    }

    constructDefaultOutput()

    if (_wasIf) {
      val primary = children[XOutput] find { _.primary == true }
      if (primary.isEmpty) {
        error(XProcException.xsPrimaryOutputRequired(location))
      }
    }

    orderChildren()
  }

  override protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    try {
      val refs = mutable.HashSet.empty[QName]

      val parser = new XValueParser(config, staticContext, _test)
      refs ++= parser.variables
      _contextDependent = parser.contextDependent

      var constant = parser.static
      for (ref <- refs) {
        val cbind = initial.inScopeConstants.get(ref)
        val dbind = initial.inScopeDynamics.get(ref)
        if (cbind.isDefined) {
          // ok
        } else if (dbind.isDefined) {
          constant = false
          dbind.get match {
            case v: XVariable =>
              _namebindings += v
            case opt: XOption =>
              _namebindings += opt
            case _ =>
              error(XProcException.xiThisCantHappen(s"Unexpected name binding: ${dbind.get}"))
          }
        } else {
          error(XProcException.xsNoBindingInExpression(ref, None))
        }
      }

      if (exceptions.nonEmpty) {
        return initial
      }

      if (constant) {
        drp = None // If it's constant, there's no need for an input

        val expr = new XProcXPathExpression(staticContext, _test)
        val bindings = mutable.HashMap.empty[String,Message]
        for ((name,value) <- initial.inScopeConstants) {
          bindings.put(name.getClarkName, value.constantValue.get)
        }

        try {
          val constantVal = config.expressionEvaluator.value(expr, List(), bindings.toMap, None)
          _constantValue = Some(constantVal)
        } catch {
          case ex: Exception =>
            if (withinTryCatch) {
              // nevermind, just let it go bang later
            } else {
              throw ex
            }
        }
      }
    } catch {
      case ex: Exception =>
        error(ex)
    }

    super.elaborateNameBindings(initial)
  }

  override def elaboratePortConnections(): Unit = {
    val input = children[XWithInput].headOption
    if (input.isDefined) {
      for (binding <- _namebindings) {
        input.get.addChild(new XNamePipe(binding))
      }
    } else {
      if (_contextDependent || _namebindings.nonEmpty) {
        val newinput = new XWithInput(parent.get, "condition")
        newinput.staticContext = staticContext
        newinput.parent = this

        if (parent.get.children[XWithInput].nonEmpty) {
          for (ds <- parent.get.children[XWithInput].head.children[XDataSource]) {
            ds match {
              case pipe: XPipe =>
                val newpipe = new XPipe(pipe)
                newinput.addChild(newpipe)
              case _ =>
                error(XProcException.xiThisCantHappen("Choose with-input is not a pipe?"))
            }
          }
        }

        for (binding <- _namebindings) {
          val pipe = new XPipe(newinput, binding.tumble_id, "result")
          newinput.addChild(pipe)
        }

        insertBefore(newinput, allChildren.head)
      }
    }

    super.elaboratePortConnections()
  }

}
