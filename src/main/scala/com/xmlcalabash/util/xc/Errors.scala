package com.xmlcalabash.util.xc

import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.`type`.ValidationFailure
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class Errors(val config: XMLCalabashConfig) {
  private val builder = new SaxonTreeBuilder(config)
  private val openStack = mutable.Stack.empty[QName]
  private val inLibrary = false

  builder.startDocument(None)

  private var nsmap = NamespaceMap.emptyMap()
  nsmap = nsmap.put("c", XProcConstants.ns_c)
  nsmap = nsmap.put("err", XProcConstants.ns_xqt_errors)

  builder.addStartElement(XProcConstants.c_errors, EmptyAttributeMap.getInstance(), nsmap)
  openStack.push(XProcConstants.c_errors)

  def endErrors(): XdmNode = {
    builder.addEndElement()
    openStack.pop()
    builder.endDocument()
    builder.result
  }

  private def end(): Unit = {
    builder.addEndElement()
    openStack.pop()
  }

  def xsdValidationError(msg: String, fail: ValidationFailure): Unit = {
    var nsmap = NamespaceMap.emptyMap()
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._message, msg))

    if (Option(fail.getSystemId).isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._source_uri, fail.getSystemId))
    }

    if (fail.getLineNumber > 0) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._source_line, fail.getLineNumber.toString))
      if (fail.getColumnNumber > 0) {
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._source_column, fail.getColumnNumber.toString))
      }
    }

    if (Option(fail.getAbsolutePath).isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._path, fail.getAbsolutePath.toString))
    }

    if (Option(fail.getErrorCodeQName).isDefined) {
      nsmap = nsmap.put("err", fail.getErrorCodeQName.getURI)
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._code, fail.getErrorCodeQName.toString))
    }

    if (Option(fail.getSchemaPart).isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._schema_part, fail.getSchemaPart.toString))
    }

    if (Option(fail.getConstraintName).isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._constraint_name, fail.getConstraintName))
    }

    if (Option(fail.getConstraintClauseNumber).isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._constraint_cause, fail.getConstraintClauseNumber))
    }

    if (Option(fail.getSchemaType).isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._schema_type, fail.getSchemaType.toString))
    }

    builder.addStartElement(XProcConstants.c_error, amap, nsmap)
    builder.addEndElement()
  }

  def xsdValidationError(msg: String): Unit = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._message, msg))

    builder.addStartElement(XProcConstants.c_error, amap)
    builder.addEndElement()
  }
}
