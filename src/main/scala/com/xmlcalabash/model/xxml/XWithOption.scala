package com.xmlcalabash.model.xxml

import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants, XValueParser}
import com.xmlcalabash.runtime.{XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.steps.internal.ValueComputation
import com.xmlcalabash.util.{S9Api, TypeUtils}
import net.sf.saxon.ma.arrays.ArrayItemType
import net.sf.saxon.ma.map.{MapItem, MapType}
import net.sf.saxon.s9api.{QName, XdmMap}

import java.net.URISyntaxException
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XWithOption(config: XMLCalabash) extends XNameBinding(config) {
  protected[xxml] def this(parent: XArtifact, name: QName, avt: Option[String], select: Option[String]) = {
    this(parent.config)

    if ((avt.isDefined && select.isDefined) || (avt.isEmpty && select.isEmpty)) {
      throw XProcException.xiThisCantHappen("Exactly one of avt or select must be defined on XWithOption")
    }

    this.parent = parent
    staticContext = parent.staticContext
    _synthetic = true
    _name = name
    _avt = avt
    _select = select
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    _href = attr(XProcConstants._href)
    _pipe = attr(XProcConstants._pipe)
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    val stepdecl = stepDeclaration
    if (stepdecl.isDefined) {
      val optdecl = stepdecl.get.option(name)
      if (optdecl.isEmpty) {
        error(XProcException.xsUndeclaredOption(stepdecl.get.stepType.get, name, location))
      } else if (optdecl.get.static) {
        error(XProcException.xsRedeclareStatic(name, location))
      }
    }

    // Any option shortcut will have been marked as an AVT.
    // But if the actual option type is a map or array, that's not really an AVT!
    if (_avt.isDefined) {
      if (stepdecl.isDefined) {
        val optdecl = stepdecl.get.option(name).get
        if (optdecl.declaredType.isDefined) {
          val opttype = optdecl.declaredType.get.getItemType
          opttype.getUnderlyingItemType match {
            case _: MapType =>
              _select = _avt
              _avt = None
              _qnameKeys = optdecl.qnameKeys
            case _: ArrayItemType =>
              _select = _avt
              _avt = None
            case _ =>
              ()
          }
        }
      }
    }

    allChildren = validateExplicitConnections(_href, _pipe)
    _href = None
    _pipe = None
  }

  protected[xxml] def elaborateDynamicOptions(): Unit = {
    if (constantValue.isDefined) {
      return
    }

    val container = ancestorContainer.get
    val compute = if (_avt.isDefined) {
      new ValueComputation(container, name, XValueParser.parseAvt(_avt.get), collection)
    } else {
      new ValueComputation(container, name, _select.get, collection)
    }

    compute.dependsOn ++= parent.get.asInstanceOf[XStep].dependsOn

    _computeValue = Some(compute)

    val xwi_source = new XWithInput(compute, "source")
    val xwi_bindings = new XWithInput(compute, "#bindings")
    val optchildren = ListBuffer.empty[XArtifact]
    for (child <- allChildren) {
      child match {
        case xi: XWithInput =>
          optchildren ++= xi.allChildren
        case _ =>
          optchildren += child
      }
    }
    allChildren = List()

    for (child <- optchildren) {
      child match {
        case nb: XNamePipe =>
          val pipe = new XPipe(this, nb.binding.tumble_id, "result")
          xwi_bindings.addChild(pipe)
        case pipe: XPipe =>
          xwi_source.addChild(pipe)
        case _ =>
          throw XProcException.xiThisCantHappen(s"Children of p:with-option include ${child}?")
      }
    }

    if (xwi_source.allChildren.nonEmpty) {
      compute.addChild(xwi_source)
    }

    if (xwi_bindings.allChildren.nonEmpty) {
      compute.addChild(xwi_bindings)
    }

    val xwo = new XWithOutput(compute, "result")
    compute.addChild(xwo)

    var bwi = parent.get.children[XWithInput] find { _.port == "#bindings" }
    if (bwi.isEmpty) {
      bwi = Some(new XWithInput(this, "#bindings"))
      addChild(bwi.get)
    }
    bwi.get.addChild(new XPipe(bwi.get, compute.stepName, "result"))

    container.insertBefore(compute, parent.get)

    xwi_source.validate()
    xwi_bindings.validate()
    xwo.validate()
  }

  // =======================================================================================

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    if (Option(_name).isDefined) {
      attr.put("name", Some(_name.getEQName))
    }

    if (constantValue.isDefined) {
      attr.put("constant-value", Some(constantValue.get.item.toString))
    } else {
      attr.put("select", _select)
      attr.put("avt", _avt)
    }

    if (drp.isDefined) {
      attr.put("drp", Some(drp.get.tumble_id))
    }

    dumpTree(sb, "p:with-option", attr.toMap)
  }

  override def toString: String = {
    if (constantValue.isDefined) {
      s"${name}: ${constantValue.get.item} (constant)"
    } else if (_avt.isDefined) {
      s"${name}: ${_avt.get} (avt)"
    } else {
      if (_select.isDefined) {
        s"${name}: ${_select.get} (select)"
      } else {
        s"${name}: ???"
      }
    }
  }
}
