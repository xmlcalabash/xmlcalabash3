package com.xmlcalabash.steps.file

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.stores.{DataInfo, FallbackDataStore, FileDataStore}
import com.xmlcalabash.util.{MediaType, TypeUtils, URIUtils}
import net.sf.saxon.om.{AttributeMap, SingletonAttributeMap}
import net.sf.saxon.s9api.{QName, XdmAtomicValue}

import java.net.URI
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

class DirectoryList() extends DefaultXmlStep {
  private val _detailed = new QName("", "detailed")
  private val _include_filter = new QName("", "include-filter")
  private val _exclude_filter = new QName("", "exclude-filter")
  private val FILE = new XdmAtomicValue("file")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val builder = new SaxonTreeBuilder(config)

    val path = if (context.baseURI.isDefined) {
      context.baseURI.get.resolve(stringBinding(XProcConstants._path))
    } else {
      new URI(stringBinding(XProcConstants._path))
    }

    builder.startDocument(path)
    builder.addStartElement(XProcConstants.c_directory)


    val detailed = booleanBinding(_detailed).getOrElse(false)
    val fileDS = new FileDataStore(config.config, new FallbackDataStore())
    val include = ListBuffer.empty[Regex]
    val exclude = ListBuffer.empty[Regex]

    if (definedBinding(_include_filter)) {
      val filter = bindings(_include_filter).value
      val iter = filter.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        include += new Regex(item.getStringValue)
      }
    }

    if (definedBinding(_exclude_filter)) {
      val filter = bindings(_exclude_filter).value
      val iter = filter.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        exclude += new Regex(item.getStringValue)
      }
    }

    fileDS.listEachEntry(path.toString, context.baseURI.getOrElse(URIUtils.cwdAsURI), "*/*", new DataInfo() {
      override def list(id: URI, props: Map[String, XdmAtomicValue]): Unit = {
        var filename = id.getPath
        if (filename.endsWith("/")) {
          filename = filename.substring(0, filename.length - 1)
        }
        val pos = filename.lastIndexOf("/")
        if (pos >= 0) {
          filename = filename.substring(pos+1)
        }

        var rel = Option.empty[String]
        var rematch = true
        if (include.nonEmpty) {
          rematch = false
          for (patn <- include) {
            filename match {
              case patn(_*) =>
                if (!rematch) {
                  rel = Some(patn.toString())
                }
                rematch = true
              case _ => ()
            }
          }
        }

        for (patn <- exclude) {
          filename match {
            case patn(_*) =>
              if (rematch) {
                rel = Some(patn.toString())
              }
              rematch = false
            case _ => ()
          }
        }

        if (rel.isDefined) {
          if (rematch) {
            logger.trace(s"Include $filename (matches ${rel.get})")
          } else {
            logger.trace(s"Exclude $filename (matches ${rel.get})")
          }
        }

        if (rematch) {
          val ename = props.getOrElse("file-type", FILE).toString match {
            case "file" => XProcConstants.c_file
            case "directory" => XProcConstants.c_directory
            case _ => XProcConstants.c_other
          }

          var amap: AttributeMap = SingletonAttributeMap.of(TypeUtils.attributeInfo(XProcConstants._name, id.toASCIIString))
          if (detailed) {
            for ((key, value) <- props) {
              if (key != "file-type") {
                amap = amap.put(TypeUtils.attributeInfo(new QName("", key), value.toString))
              }
            }
          }
          builder.addStartElement(ename, amap)
          builder.addEndElement()
        }
      }
    })

    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML, path))
  }
}
