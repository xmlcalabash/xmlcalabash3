package com.xmlcalabash.steps.json

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap, XdmNode, XdmValue}

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{IteratorHasAsJava, MapHasAsScala}


class Merge extends DefaultXmlStep {
  private val _duplicates = new QName("", "duplicates")
  private val _key = new QName("", "key")
  private val p_index = new QName("p", XProcConstants.ns_p, "index")
  private val inputs = ListBuffer.empty[XdmItem]

  private var duplicates: String = _
  private var merged: XdmMap = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.JSONSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.JSONRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case xdm: XdmMap => inputs += xdm
      case item: XdmItem => inputs += item
      case _ => throw new RuntimeException("Unexpected document type passed to p:json-merge")
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    super.run(staticContext)

    duplicates = bindings(_duplicates).value.getUnderlyingValue.getStringValue
    val keyExpr = bindings(_key).value.getUnderlyingValue.getStringValue
    var index = 0

    merged = new XdmMap()
    for (item <- inputs) {
      index += 1

      item match {
        case map: XdmMap =>
          for ((key,value) <- map.asImmutableMap().asScala) {
            addToMap(key, value)
          }
        case _ =>
          val compiler = config.processor.newXPathCompiler()
          if (staticContext.baseURI.isDefined) {
            compiler.setBaseURI(staticContext.baseURI.get)
          }
          for ((pfx, uri) <- staticContext.nsBindings) {
            compiler.declareNamespace(pfx, uri)
          }
          compiler.declareVariable(p_index)
          val expr = compiler.compile(keyExpr)
          val selector = expr.load()
          selector.setVariable(p_index, new XdmAtomicValue(index))
          selector.setContextItem(item)
          val value = selector.evaluate()
          value match {
            case atomic: XdmAtomicValue =>
              addToMap(atomic, item.asInstanceOf[XdmValue])
            case _: XdmNode =>
              addToMap(new XdmAtomicValue(value.getUnderlyingValue.getStringValue), item.asInstanceOf[XdmValue])
            case _ =>
              try {
                throw XProcException.xcInvalidJsonMergeKey(value.getUnderlyingValue.getStringValue, location)
              } catch {
                case ex: Exception =>
                  throw XProcException.xcInvalidJsonMergeKey(location)
              }
          }
      }
    }

    consumer.get.receive("result", merged, XProcMetadata.JSON)
  }

  private def addToMap(key: XdmAtomicValue, value: XdmValue): Unit = {
    if (merged.containsKey(key)) {
      duplicates match {
        case "reject" =>
          throw XProcException.xcRejectDuplicateKeys(key.getStringValue, location)
        case "use-first" =>
          ()
        case "use-last" =>
          merged = merged.put(key, value)
        case "use-any" =>
          ()
        case _ => // combine
          val curValue = merged.get(key)
          val vlist = ListBuffer.empty[XdmItem]
          val iter = curValue.iterator()
          while (iter.hasNext) {
            vlist += iter.next()
          }
          vlist += value.asInstanceOf[XdmItem]
          merged = merged.put(key, new XdmValue(vlist.iterator.asJava))
      }
    } else {
      merged = merged.put(key, value)
    }
  }

  override def reset(): Unit = {
    super.reset()
    inputs.clear()
  }
}
