package com.xmlcalabash.model.util

import com.jafpl.graph.Location
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants.ValueTemplate
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime, XProcExpression, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.util.{MinimalStaticContext, TypeUtils, ValueTemplateParser}
import net.sf.saxon.s9api.{Axis, ItemType, QName, XdmAtomicValue, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable

object ValueParser {
  def parseAvt(value: String): Option[ValueTemplate] = {
    val parser = new ValueTemplateParser(value)
    Some(parser.template())
  }

  def findVariableRefsInString(config: XMLCalabashRuntime, text: String, context: StaticContext): Set[QName] = {
    val names = mutable.HashSet.empty[QName]

    val parser = config.expressionParser
    parser.parse(text)
    for (ref <- parser.variableRefs) {
      val qname = parseQName(ref, context)
      names += qname
    }

    names.toSet
  }

  def parseClarkName(name: String): QName = {
    parseClarkName(name, None)
  }

  def parseClarkName(name: String, prefix: String): QName = {
    parseClarkName(name, Some(prefix))
  }

  private def parseClarkName(name: String, pfx: Option[String]): QName = {
    // FIXME: Better error handling for ClarkName parsing
    if (name.startsWith("{")) {
      val pos = name.indexOf("}")
      val uri = name.substring(1, pos)
      val local = name.substring(pos + 1)
      if (pfx.isDefined) {
        new QName(pfx.get, uri, local)
      } else {
        new QName(uri, local)
      }
    } else {
      new QName("", name)
    }
  }

  def parseQName(name: String, context: StaticContext): QName = {
    parseQName(Some(name), context).get
  }

  def parseQName(name: Option[String], context: StaticContext): Option[QName] = {
    if (name.isDefined) {
      val eqname = "^Q\\s*\\{(.*)\\}(\\S+)$".r
      name.get match {
        case eqname(uri,local) => Some(new QName(uri, local))
        case _ =>
          if (name.get.contains(":")) {
            val pos = name.get.indexOf(':')

            val prefix = TypeUtils.castAtomicAs(new XdmAtomicValue(name.get.substring(0, pos)), ItemType.NCNAME, context).getStringValue
            val local = TypeUtils.castAtomicAs(new XdmAtomicValue(name.get.substring(pos+1)), ItemType.NCNAME, context).getStringValue

            if (context.nsBindings.contains(prefix)) {
              Some(new QName(prefix, context.nsBindings(prefix), local))
            } else {
              throw XProcException.xdCannotResolveQName(name.get, context.location)
            }
          } else {
            val local = TypeUtils.castAtomicAs(new XdmAtomicValue(name.get), ItemType.NCNAME, context).getStringValue
            Some(new QName("", local))
          }
      }
    } else {
      None
    }
  }

  def parseBoolean(value: Option[String], location: Option[Location]): Option[Boolean] = {
    parseBoolean(value, location, false)
  }

  def parseBoolean(value: Option[String], location: Option[Location], static: Boolean): Option[Boolean] = {
    if (value.isDefined) {
      if (value.get == "true" || value.get == "false") {
        Some(value.get == "true")
      } else {
        if (static) {
          throw XProcException.xsBadTypeValue(value.get, "boolean", location)
        } else {
          throw XProcException.xdBadType(value.get, "boolean", location)
        }
      }
    } else {
      None
    }
  }


  def parseParameters(value: XdmValue, context: MinimalStaticContext): Map[QName, XdmValue] = {
    val params = mutable.HashMap.empty[QName, XdmValue]

    value match {
      case map: XdmMap =>
        // Grovel through a Java Map
        val iter = map.keySet().iterator()
        while (iter.hasNext) {
          val key = iter.next()
          val value = map.get(key)

          val qname = context.parseQName(key.getStringValue)
          params.put(qname, value)
        }
      case _ =>
        throw XProcException.xiParamsNotMap(context.location, value)
    }

    params.toMap
  }

  def parseDocumentProperties(value: XdmItem, context: MinimalStaticContext, location: Option[Location]): Map[QName, XdmValue] = {
    val params = mutable.HashMap.empty[QName, XdmValue]

    value match {
      case map: XdmMap =>
        // Grovel through a Java Map
        val iter = map.keySet().iterator()
        while (iter.hasNext) {
          val key = iter.next()
          val value = map.get(key)

          val keytype = key.getTypeName
          val qkey = keytype match {
            case XProcConstants.xs_QName =>
              key.getQNameValue
            case XProcConstants.xs_string =>
              context.parseQName(key.getStringValue)
            case _ =>
              throw XProcException.xdBadMapKey(key.getStringValue, location)
          }

          params.put(qkey, value)
        }
      case _ =>
        throw XProcException.xiDocPropsNotMap(location, value)
    }

    params.toMap
  }

  def findVariableRefs(config: XMLCalabashRuntime, expression: XProcExpression, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    expression match {
      case expr: XProcXPathExpression =>
        val parser = config.expressionParser
        parser.parse(expr.expr)
        for (ref <- parser.variableRefs) {
          val qname = ValueParser.parseClarkName(ref)
          variableRefs += qname
        }
      case expr: XProcVtExpression =>
        var avt = false
        for (subexpr <- expr.avt) {
          if (avt) {
            val parser = config.expressionParser
            parser.parse(subexpr)
            for (ref <- parser.variableRefs) {
              val qname = ValueParser.parseClarkName(ref)
              variableRefs += qname
            }
          }
          avt = !avt
        }
      case _ =>
        throw XProcException.xiUnkExprType(location)
    }

    variableRefs.toSet
  }

  def findVariableRefs(config: XMLCalabashRuntime, node: XdmNode, expandText: Boolean, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    node.getNodeKind match {
      case XdmNodeKind.ELEMENT =>
        var newExpand = expandText
        var iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val attr = iter.next()
          if (expandText) {
            variableRefs ++= ValueParser.findVariableRefsInTvt(config, attr.getStringValue, location)
          }
          if (attr.getNodeName == XProcConstants.p_expand_text) {
            newExpand = ValueParser.parseBoolean(Some(attr.getStringValue), location).get
          }
        }
        iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next()
          variableRefs ++= ValueParser.findVariableRefs(config, child, newExpand, location)
        }
      case XdmNodeKind.TEXT =>
        if (expandText) {
          variableRefs ++= ValueParser.findVariableRefsInTvt(config, node.getStringValue, location)
        }
      case _ => ()
    }

    variableRefs.toSet
  }

  private def findVariableRefsInTvt(config: XMLCalabashRuntime, text: String, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    val list = ValueParser.parseAvt(text)
    if (list.isEmpty) {
      throw new ModelException(ExceptionCode.BADAVT, List("TVT", text), location)
    }

    findVariableRefsInAvt(config, list.get)
  }

  def findVariableRefsInAvt(config: XMLCalabashRuntime, list: List[String]): Set[QName] = {
    findVariableRefsInAvt(config.expressionParser, list)
  }

  def findVariableRefsInAvt(config: XMLCalabash, list: List[String]): Set[QName] = {
    findVariableRefsInAvt(config.expressionParser, list)
  }

  private def findVariableRefsInAvt(parser: ExpressionParser, list: List[String]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    var avt = false
    for (substr <- list) {
      if (avt) {
        variableRefs ++= ValueParser.findVariableRefsInString(parser, substr)
      }
      avt = !avt
    }

    variableRefs.toSet
  }

  def findVariableRefsInString(config: XMLCalabash, text: String): Set[QName] = {
    findVariableRefsInString(config.expressionParser, text)
  }

  def findVariableRefsInString(config: XMLCalabashRuntime, text: String): Set[QName] = {
    findVariableRefsInString(config.expressionParser, text)
  }

  private def findVariableRefsInString(parser: ExpressionParser, text: String): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    parser.parse(text)
    for (ref <- parser.variableRefs) {
      val qname = ValueParser.parseClarkName(ref)
      variableRefs += qname
    }

    variableRefs.toSet
  }

  object StateChange {
    val STRING = 0
    val EXPR = 1
  }
}
