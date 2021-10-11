package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.XMLContext
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable

class PropertyMerge extends DefaultXmlStep {
  private var sourceDoc = Option.empty[Any]
  private var sourceMeta = Option.empty[XProcMetadata]
  private var propDoc = Option.empty[Any]
  private var propMeta = Option.empty[XProcMetadata]
  private var prop = Option.empty[Map[QName,XdmValue]]

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE, "properties" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("*"), "properties" -> List("application/xml")))
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      sourceDoc = Some(item)
      sourceMeta = Some(metadata)
    } else { // port="properties"
      propDoc = Some(item)
      propMeta = Some(metadata)
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    super.run(staticContext)

    propDoc.get match {
      case node: XdmNode =>
        val piter = node.axisIterator(Axis.CHILD)
        while (piter.hasNext) {
          val pnode = piter.next()
          pnode.getNodeKind match {
            case XdmNodeKind.ELEMENT =>
              if (pnode.getNodeName == XProcConstants.c_document_properties) {
                prop = Some(extractProperties(staticContext, pnode))
              } else {
                throw XProcException.xiMergeBadRoot(pnode.getNodeName, location)
              }
            case XdmNodeKind.TEXT =>
              if (pnode.getStringValue.trim != "") {
                throw XProcException.xiMergeBadText(pnode.getStringValue.trim, location)
              }
            case _ => ()
          }
        }

      case _ => throw XProcException.xiMergeBadValue(propDoc, location)
    }

    val newmeta = new XProcMetadata(sourceMeta.get.contentType, prop.get)
    consumer.get.receive("result", sourceDoc.get, newmeta)
  }

  private def extractProperties(context: StaticContext, node: XdmNode): Map[QName,XdmValue] = {
    val prop = mutable.HashMap.empty[QName,XdmValue]

   val piter = node.axisIterator(Axis.CHILD)
    while (piter.hasNext) {
      val pnode = piter.next()
      pnode.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          val name = pnode.getNodeName
          if (prop.contains(name)) {
            throw XProcException.xiMergeDup(name, location)
          }

          val vtypestr = Option(pnode.getAttributeValue(XProcConstants.xsi_type))
          val vtype = if (vtypestr.isDefined) {
            val ns = S9Api.inScopeNamespaces(pnode)
            val scontext = new XMLContext(config.config)
            scontext.nsBindings = ns
            if (location.isDefined) {
              scontext.location = location.get
            }
            Some(ValueParser.parseQName(vtypestr.get, scontext))
          } else {
            None
          }

          var strvalue = ""
          var count = 0
          var atomic = true
          var viter = pnode.axisIterator(Axis.CHILD)
          while (viter.hasNext) {
            val vnode = viter.next()
            if (vnode.getNodeKind == XdmNodeKind.TEXT) {
              strvalue = vnode.getStringValue
            } else {
              atomic = false
            }
            count += 1
            atomic = atomic && count == 1
          }

          if (!atomic && vtype.isDefined) {
            throw XProcException.xiMergeXsiTypeOnNode(location)
          }

          if (atomic) {
            if (vtype.isDefined) {
              vtype.get match {
                case XProcConstants.xs_string =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_boolean =>
                  prop.put(name, new XdmAtomicValue(strvalue.toBoolean))
                case XProcConstants.xs_duration =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_dateTime =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_date =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_time =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gYearMonth =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gMonth =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gMonthDay =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gYear =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gDay =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_hexBinary =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_base64Binary =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_anyURI =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_QName =>
                  val scontext = new XMLContext(config.config)
                  scontext.nsBindings = S9Api.inScopeNamespaces(node)
                  if (location.isDefined) {
                    scontext.location = location.get
                  }
                  prop.put(name, new XdmAtomicValue(ValueParser.parseQName(strvalue,scontext)))
                case XProcConstants.xs_notation =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_decimal =>
                  prop.put(name, new XdmAtomicValue(strvalue.toDouble)) // FIXME: xs:decimal isn't double...
                case XProcConstants.xs_float =>
                  prop.put(name, new XdmAtomicValue(strvalue.toDouble))
                case XProcConstants.xs_double =>
                  prop.put(name, new XdmAtomicValue(strvalue.toDouble))
                case XProcConstants.xs_integer =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_nonPositiveInteger =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_negativeInteger =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_long =>
                  prop.put(name, new XdmAtomicValue(strvalue.toLong))
                case XProcConstants.xs_int =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_short =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_byte =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_nonNegativeInteger =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_positiveInteger =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_unsignedLong =>
                  prop.put(name, new XdmAtomicValue(strvalue.toLong))
                case XProcConstants.xs_unsignedInt =>
                  prop.put(name, new XdmAtomicValue(strvalue.toLong))
                case XProcConstants.xs_unsignedShort =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_unsignedByte =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_yearMonthDuration =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_dayTimeDuration =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_normalizedString =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_token =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_name =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_NMTOKEN =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_NCName =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_ID =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_IDREF =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_ENTITY =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_dateTimeStamp =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case _ =>
                  throw XProcException.xiMergeBadAtomic(vtype.get, location)
              }
            } else {
              val x = XProcConstants._code
              prop.put(name, new XdmAtomicValue(strvalue))
            }
          } else {
            val builder = new SaxonTreeBuilder(config)
            builder.startDocument(node.getBaseURI)
            viter = node.axisIterator(Axis.CHILD)
            while (viter.hasNext) {
              val vnode = viter.next()
              builder.addSubtree(vnode)
            }
            builder.endDocument()
            prop.put(name, builder.result)
          }

        case XdmNodeKind.TEXT =>
          if (pnode.getStringValue.trim != "") {
            throw XProcException.xiMergeBadText(pnode.getStringValue.trim, location)
          }
        case _ => ()
      }
    }

    prop.put(XProcConstants._content_type, sourceMeta.get.properties(XProcConstants._content_type))

    // FIXME: figure out how to standardize base-uri
    if (sourceMeta.get.properties.contains(XProcConstants._base_uri)) {
      prop.put(XProcConstants._base_uri, sourceMeta.get.properties(XProcConstants._base_uri))
    }

    prop.toMap
  }
}
