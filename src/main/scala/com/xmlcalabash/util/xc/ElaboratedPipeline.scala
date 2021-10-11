package com.xmlcalabash.util.xc

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, NamespaceMap, SingletonAttributeMap}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class ElaboratedPipeline(config: XMLCalabashConfig) {
  private val builder = new SaxonTreeBuilder(config)
  private val openStack = mutable.Stack.empty[QName]
  private val inLibrary = false

  def startPipeline(tumble_id: String, stepName: String, stepType: Option[QName], version: Double): Unit = {
    startPipeline(tumble_id, stepName, stepType, version, None, None, None, None)
  }

  def startPipeline(tumble_id: String, stepName: String,
                    stepType: Option[QName],
                    version: Double,
                    psviRequired: Option[Boolean],
                    xpathVersion: Option[Double],
                    excludePrefixes: Option[String],
                    visibility: Option[String]): Unit = {
    if (!inLibrary) {
      builder.startDocument(None)
    }
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))
    amap = oattr(amap, XProcConstants._type, stepType)
    amap = oattr(amap, XProcConstants._version, Some(version))
    amap = oattr(amap, XProcConstants._xpath_version, xpathVersion)
    amap = oattr(amap, XProcConstants._psvi_required, psviRequired)
    amap = oattr(amap, XProcConstants._exclude_inline_prefixes, excludePrefixes)
    amap = oattr(amap, XProcConstants._visibility, visibility)

    var nsmap = NamespaceMap.emptyMap()
    nsmap = nsmap.put("p", XProcConstants.ns_p)
    nsmap = nsmap.put("cx", XProcConstants.ns_cx)

    builder.addStartElement(XProcConstants.p_declare_step, amap, nsmap)
    openStack.push(XProcConstants.p_declare_step)
  }

  def endPipeline(): Option[XdmNode] = {
    builder.addEndElement()
    openStack.pop()

    if (inLibrary) {
      None
    } else {
      builder.endDocument()
      Some(builder.result)
    }
  }

  private def end(): Unit = {
    builder.addEndElement()
    openStack.pop()
  }

  def startInput(tumble_id: String, name: String, port: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, name))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._port, port))

    val element = XProcConstants.p_input
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endInput(): Unit = {
    end()
  }

  def startOutput(tumble_id: String, name: String, port: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, name))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._port, port))

    val element = XProcConstants.p_output
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endOutput(): Unit = {
    end()
  }

  def startWithOutput(tumble_id: String, name: String, port: String, sequence: Boolean): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, name))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._port, port))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._sequence, sequence.toString))

    val element = XProcConstants.p_with_output
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endWithOutput(): Unit = {
    end()
  }

  def startWithInput(tumble_id: String, name: String, port: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, name))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._port, port))

    val element = XProcConstants.p_with_input
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endWithInput(): Unit = {
    end()
  }

  def startPipe(tumble_id: String, step: String, port: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._step, step))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._port, port))

    val element = XProcConstants.p_pipe
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endPipe(): Unit = {
    end()
  }

  def startNamePipe(tumble_id: String, step: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._step, step))

    val element = new QName("p", XProcConstants.ns_p, "name-pipe")
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endNamePipe(): Unit = {
    end()
  }

  def startDocument(tumble_id: String, href: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._href, href))

    val element = XProcConstants.p_document
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endDocument(): Unit = {
    end()
  }

  def startEmpty(tumble_id: String): Unit = {
    var amap = tid(tumble_id)
    val element = XProcConstants.p_empty
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endEmpty(): Unit = {
    end()
  }

  def startInline(tumble_id: String, root: Option[QName]): Unit = {
    var amap = tid(tumble_id)
    if (root.isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(new QName("root"), root.get.toString))
    }

    val element = XProcConstants.p_inline
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endInline(): Unit = {
    end()
  }

  def startDocumentation(tumble_id: String, root: Option[QName]): Unit = {
    var amap = tid(tumble_id)
    if (root.isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(new QName("root"), root.get.toString))
    }

    val element = XProcConstants.p_documentation
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endDocumentation(): Unit = {
    end()
  }

  def startPipeInfo(tumble_id: String, root: Option[QName]): Unit = {
    var amap = tid(tumble_id)
    if (root.isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(new QName("root"), root.get.toString))
    }

    val element = XProcConstants.p_pipeinfo
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endPipeInfo(): Unit = {
    end()
  }

  def startVariable(tumble_id: String, stepName: String, name: QName): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))
    amap = amap.put(TypeUtils.attributeInfo(new QName("varname"), name.toString))

    val element = XProcConstants.p_variable
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endWithVariable(): Unit = {
    end()
  }

  def startOption(tumble_id: String, name: String, optname: QName): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, name))
    amap = amap.put(TypeUtils.attributeInfo(new QName("optname"), optname.toString))

    val element = XProcConstants.p_option
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endOption(): Unit = {
    end()
  }

  def startWithOption(tumble_id: String, name: String, optname: QName): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, name))
    amap = amap.put(TypeUtils.attributeInfo(new QName("optname"), optname.toString))

    val element = XProcConstants.p_with_option
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endWithOption(): Unit = {
    end()
  }

  def startAtomic(tumble_id: String,
                  stepName: String,
                  stepType: QName): Unit = {
    var amap = tid(tumble_id)

    var nsmap = NamespaceMap.emptyMap()
    nsmap = nsmap.put("p", XProcConstants.ns_p)
    nsmap = nsmap.put("cx", XProcConstants.ns_cx)
    if (stepType.getPrefix != null && !"".equals(stepType.getPrefix)) {
      nsmap = nsmap.put(stepType.getPrefix, stepType.getNamespaceURI)
    }

    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = stepType
    builder.addStartElement(element, amap, nsmap)
    openStack.push(element)
  }

  def endAtomic(): Unit = {
    end()
  }

  def startViewport(tumble_id: String,
                    stepName: String,
                    pattern: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._match, pattern))

    val element = XProcConstants.p_viewport
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endViewport(): Unit = {
    end()
  }

  def startForEach(tumble_id: String,
                   stepName: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = XProcConstants.p_for_each
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endForEach(): Unit = {
    end()
  }

  def startForUntil(tumble_id: String,
                    stepName: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = XProcConstants.cx_until
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endForUntil(): Unit = {
    end()
  }

  def startForWhile(tumble_id: String,
                    stepName: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = XProcConstants.cx_while
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endForWhile(): Unit = {
    end()
  }

  def startForLoop(tumble_id: String,
                    stepName: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = XProcConstants.cx_loop
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endForLoop(): Unit = {
    end()
  }

  def startChoose(tumble_id: String,
                  stepName: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = XProcConstants.p_choose
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endChoose(): Unit = {
    end()
  }

  def startWhen(tumble_id: String,
                stepName: String,
                test: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._test, test))

    val element = XProcConstants.p_when
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endWhen(): Unit = {
    end()
  }

  def startGroup(tumble_id: String,
                   stepName: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = XProcConstants.p_group
    builder.addStartElement(element)
    openStack.push(element)
  }

  def endGroup(): Unit = {
    end()
  }

  def startTry(tumble_id: String,
               stepName: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = XProcConstants.p_try
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endTry(): Unit = {
    end()
  }

  def startCatch(tumble_id: String,
                 stepName: String,
                 codes: Option[String]): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))
    amap = oattr(amap, XProcConstants._code, Some(codes))

    val element = XProcConstants.p_catch
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endCatch(): Unit = {
    end()
  }

  def startFinally(tumble_id: String,
                    stepName: String): Unit = {
    var amap = tid(tumble_id)
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, stepName))

    val element = XProcConstants.p_catch
    builder.addStartElement(element, amap)
    openStack.push(element)
  }

  def endFinally(): Unit = {
    end()
  }

  private def oattr(amap: AttributeMap, name: QName, value: Option[Any]): AttributeMap = {
    if (value.isDefined) {
      amap.put(TypeUtils.attributeInfo(name, value.get.toString))
    } else {
      amap
    }
  }

  private def tid(id: String): AttributeMap = {
    val xid = s"ID_${id.substring(1)}"
    SingletonAttributeMap.of(TypeUtils.attributeInfo(XProcConstants.xml_id, xid))
  }
}
