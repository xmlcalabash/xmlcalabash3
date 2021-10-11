package com.xmlcalabash.steps.os

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

class OsInfo extends DefaultXmlStep {
  // I should make this static...
  private val nameMap = mutable.HashMap.empty[String, String]
  nameMap.put("file.separator", "file-separator")
  nameMap.put("path.separator", "path-separator")
  nameMap.put("os.arch", "os-architecture")
  nameMap.put("os.name", "os-name")
  nameMap.put("os.version", "os-version")
  nameMap.put("user.dir", "cwd")
  nameMap.put("user.name", "user-name")
  nameMap.put("user.home", "user-home")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    for (property <- System.getProperties.asScala.keySet) {
      val pname = property.toString
      val value = System.getProperty(pname)
      try {
        val name = if (nameMap.contains(pname)) {
          new QName("", nameMap(pname))
        } else {
          // Substituting "-" for "." seems to make the list more consistent
          new QName("", pname.replace(".", "-"))
        }
        amap = amap.put(TypeUtils.attributeInfo(name, value))
      } catch {
        case _: Exception =>
          // Nevermind, if the property name can't be a QName, I give up
          ()
      }
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_result, amap)

    for (name <- System.getenv().asScala.keySet) {
      val value = System.getenv(name)
      var emap: AttributeMap = EmptyAttributeMap.getInstance()
      emap = emap.put(TypeUtils.attributeInfo(XProcConstants._name, name))
      emap = emap.put(TypeUtils.attributeInfo(XProcConstants._value, value))
      builder.addStartElement(XProcConstants.c_environment, emap)
      builder.addEndElement()
    }

    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
