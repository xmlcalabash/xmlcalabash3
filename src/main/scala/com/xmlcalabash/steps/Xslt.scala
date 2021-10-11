package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{S9Api, ValueUtils, XProcCollectionFinder}
import net.sf.saxon.Configuration
import net.sf.saxon.event.{PipelineConfiguration, Receiver}
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.functions.ResolveURI
import net.sf.saxon.lib.{ResultDocumentResolver, SaxonOutputKeys}
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.{Action, Destination, MessageListener, QName, RawDestination, SaxonApiException, ValidationMode, XdmArray, XdmAtomicValue, XdmDestination, XdmEmptySequence, XdmFunctionItem, XdmItem, XdmMap, XdmNode, XdmValue}
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.trans.XPathException
import net.sf.saxon.tree.wrapper.RebasedDocument

import java.net.URI
import javax.xml.transform.{ErrorListener, SourceLocator, TransformerException}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{IteratorHasAsJava, MapHasAsJava, SetHasAsScala}
import scala.jdk.FunctionConverters.enrichAsJavaFunction

class Xslt extends DefaultXmlStep {
  private val _global_context_item = new QName("", "global-context-item")
  private val _initial_mode = new QName("", "initial-mode")
  private val _template_name = new QName("", "template-name")
  private val _output_base_uri = new QName("", "output-base-uri")

  private var stylesheet = Option.empty[XdmNode]
  private val inputSequence = ListBuffer.empty[XdmItem]
  private val inputMetadata = ListBuffer.empty[XProcMetadata]

  private var staticContext: StaticContext = _
  private var globalContextItem = Option.empty[XdmValue]
  private var initialMode = Option.empty[QName]
  private var templateName = Option.empty[QName]
  private var outputBaseURI = Option.empty[String]
  private var parameters = Map.empty[QName, XdmValue]
  private var staticParameters = Map.empty[QName, XdmValue]
  private var populateDefaultCollection = true
  private var version = Option.empty[String]

  private var goesBang = Option.empty[XProcException]

  private var primaryDestination: Destination = _
  private var primaryOutputProperties: Map[QName, XdmValue] = _
  private val secondaryResults = mutable.HashMap.empty[URI, Destination]
  private val secondaryOutputProperties = mutable.HashMap.empty[URI, Map[QName, XdmValue]]

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ZERO_OR_MORE, "stylesheet" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/xml", "text/plain"), "stylesheet" -> List("application/xml"))
  )

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.ZERO_OR_MORE, "secondary" -> PortCardinality.ZERO_OR_MORE),
    Map("result" -> List("application/xml", "text/plain"),
      "secondary" -> List("application/xml", "text/plain"))
  )

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    port match {
      case "source" =>
        inputMetadata += metadata
        item match {
          case b: BinaryNode =>
            inputSequence += b.node
          case n: XdmItem =>
            inputSequence += n
          case _ =>
            throw XProcException.xiThisCantHappen(s"Unexpected node type on XSLT input: ${item}", location);
        }
      case "stylesheet" =>
        stylesheet = Some(item.asInstanceOf[XdmNode])
      case _ => ()
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    super.run(staticContext)

    this.staticContext = staticContext

    var pmap = mapBinding(XProcConstants._parameters)
    if (pmap.size() > 0) {
      parameters = ValueParser.parseParameters(pmap, staticContext)
    }
    pmap = mapBinding(XProcConstants._static_parameters)
    if (pmap.size() > 0) {
      staticParameters = ValueParser.parseParameters(pmap, staticContext)
    }

    if (bindings.contains(_global_context_item)) {
      globalContextItem = Some(bindings(_global_context_item).value)
      if (globalContextItem.get.size() == 0) {
        globalContextItem = None
      }
    }

    initialMode = qnameBinding(_initial_mode)
    templateName = qnameBinding(_template_name)
    outputBaseURI = optionalStringBinding(_output_base_uri)
    version = optionalStringBinding(XProcConstants._version)
    populateDefaultCollection = booleanBinding(XProcConstants._populate_default_collection).getOrElse(populateDefaultCollection)

    if (version.isEmpty && stylesheet.isDefined) {
      val root = S9Api.documentElement(stylesheet.get)
      version = Option(root.get.getAttributeValue(XProcConstants._version))
    }

    if (version.isEmpty || !List("1.0", "2.0", "3.0").contains(version.get)) {
      throw XProcException.xcVersionNotAvailable(version.getOrElse(""), location)
    }

    version.get match {
      case "3.0" => xslt30()
      case "2.0" => xslt20()
      case "1.0" => throw XProcException.xcVersionNotAvailable(version.get, location)
      case _ => throw XProcException.xcVersionNotAvailable(version.get, location)
    }
  }

  private def xslt30(): Unit = {
    if (globalContextItem.isEmpty && inputSequence.length == 1) {
      globalContextItem = inputSequence.headOption
    }
    runXsltProcessor(inputSequence.headOption, inputMetadata.headOption)
  }

  private def xslt20(): Unit = {
    for (meta <- inputMetadata) {
      val ctype = meta.contentType
      if (!ctype.xmlContentType && !ctype.htmlContentType && !ctype.textContentType) {
        throw XProcException.xcXsltInputNot20Compatible(ctype, location)
      }
    }

    for ((name, value) <- parameters) {
      value match {
        case _: XdmAtomicValue => ()
        case _: XdmNode => ()
        case _: XdmMap =>
          throw XProcException.xcXsltInvalidParameterType(name, "map", location)
        case _: XdmArray => ()
          throw XProcException.xcXsltInvalidParameterType(name, "array", location)
        case _: XdmFunctionItem => ()
          throw XProcException.xcXsltInvalidParameterType(name, "function", location)
        case _ =>
          logger.debug(s"Unexpected parameter type: ${value} passed to p:xslt")
      }
    }

    globalContextItem = None
    runXsltProcessor(inputSequence.headOption, inputMetadata.headOption)
  }

  private def runXsltProcessor(document: Option[XdmItem], docmeta: Option[XProcMetadata]): Unit = {
    val runtime = this.config.config
    val processor = runtime.processor
    val config = processor.getUnderlyingConfiguration
    // FIXME: runtime.getConfigurer().getSaxonConfigurer().configXSLT(config);

    val collectionFinder = config.getCollectionFinder
    val unparsedTextURIResolver = config.getUnparsedTextURIResolver

    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)

    val exec = try {
      compiler.compile(stylesheet.get.asSource())
    } catch {
      case sae: Exception =>
        // Compile time exceptions are caught
        if (goesBang.isDefined) {
          throw goesBang.get
        }
        // Runtime ones are not
        val cause = if (Option(sae.getCause).isDefined) {
          sae.getCause match {
            case xe: XPathException =>
              if (Option(xe.getErrorCodeLocalPart).isDefined) {
                Some(new QName(xe.getErrorCodeNamespace, xe.getErrorCodeLocalPart))
              } else {
                None
              }
            case _ =>
              None
          }
        } else {
          None
        }

        if (cause.isDefined) {
          cause.get match {
            case XProcConstants.err_XTMM9000 =>
              throw XProcException.xcXsltUserTermination(sae.getMessage, location)
            case XProcConstants.err_XTDE0040 =>
              throw XProcException.xcXsltNoTemplate(templateName.get, location)
            case _ =>
              throw XProcException.xcXsltRuntimeError(cause.get, sae.getMessage, location)
          }
        }

        throw XProcException.xcXsltCompileError(sae.getMessage, sae, location)
    }

    val transformer = exec.load30()
    transformer.setResultDocumentHandler(new DocumentHandler().asJava)
    transformer.setStylesheetParameters(parameters.asJava)

    if (populateDefaultCollection) {
      transformer.getUnderlyingController.setDefaultCollection(XProcCollectionFinder.DEFAULT)
      val docs = ListBuffer.empty[XdmNode]
      for (value <- inputSequence) {
        value match {
          case node: XdmNode => docs += node
          case _ => ()
        }
      }
      transformer.getUnderlyingController.setCollectionFinder(new XProcCollectionFinder(runtime, docs.toList, collectionFinder))
    }

    val inputSelection = if (document.isDefined) {
      val iter = inputSequence.iterator.asJava
      new XdmValue(iter)
    } else {
      XdmEmptySequence.getInstance
    }

    transformer.setMessageListener(new CatchMessages())

    transformer.getUnderlyingController.setResultDocumentResolver(new MyResultDocumentResolver(processor.getUnderlyingConfiguration))

    primaryOutputProperties = S9Api.serializationPropertyMap(transformer.getUnderlyingController.getExecutable.getPrimarySerializationProperties)
    var buildTree = false
    if (primaryOutputProperties.contains(XProcConstants.BUILD_TREE)) {
      buildTree = ValueUtils.isTrue(primaryOutputProperties.get(XProcConstants.BUILD_TREE))
    } else {
      val method = primaryOutputProperties.get(XProcConstants._method)
      if (method.isDefined) {
        buildTree = List("xml", "html", "xhtml", "text").contains(method.get.toString)
      } else {
        buildTree = true
      }
    }
    primaryDestination = if (buildTree) {
      new XdmDestination()
    } else {
      new RawDestination()
    }

    if (initialMode.isDefined) {
      try {
        transformer.setInitialMode(initialMode.get)
      } catch {
        case iae: IllegalArgumentException =>
          throw XProcException.xcXsltNoMode(initialMode.get, iae.getMessage, location)
      }
    }

    if (outputBaseURI.isDefined) {
      if (staticContext.baseURI.isDefined) {
        transformer.setBaseOutputURI(staticContext.baseURI.get.resolve(outputBaseURI.get).toASCIIString)
      } else {
        transformer.setBaseOutputURI(outputBaseURI.get)
      }
    } else {
      if (document.isDefined) {
        if (docmeta.get.baseURI.isDefined) {
          transformer.setBaseOutputURI(docmeta.get.baseURI.get.toASCIIString)
        } else if (document.get.isInstanceOf[XdmNode]) {
          val base = document.get.asInstanceOf[XdmNode].getBaseURI
          transformer.setBaseOutputURI(base.toASCIIString)
        }
      } else {
        if (stylesheet.isDefined && stylesheet.get.isInstanceOf[XdmNode]) {
          transformer.setBaseOutputURI(stylesheet.get.getBaseURI.toASCIIString)
        }
      }
    }

    transformer.setSchemaValidationMode(ValidationMode.DEFAULT)
    // FIXME: transformer.getUnderlyingController().setUnparsedTextURIResolver(unparsedTextURIResolver)

    if (globalContextItem.isDefined) {
      transformer.setGlobalContextItem(globalContextItem.get.asInstanceOf[XdmItem])
    }

    try {
      if (templateName.isDefined) {
        transformer.callTemplate(templateName.get, primaryDestination)
      } else {
        transformer.applyTemplates(inputSelection, primaryDestination)
      }
    } catch {
      case ex: SaxonApiException =>
        ex.getErrorCode match {
          case XProcConstants.err_XTMM9000 =>
            throw XProcException.xcXsltUserTermination(ex.getMessage, location)
          case XProcConstants.err_XTDE0040 =>
            throw XProcException.xcXsltNoTemplate(templateName.get, location)
          case _ =>
            throw XProcException.xcXsltRuntimeError(ex.getErrorCode, ex.getMessage, location)
        }
      case ex: Exception =>
        throw XProcException.xcXsltRuntimeError(XProcConstants.err_XC0095, ex.getMessage, location)
    }

    primaryDestination match {
      case raw: RawDestination =>
        val iter = raw.getXdmValue.iterator()
        var result = Option.empty[XdmValue]
        while (iter.hasNext) {
          val next = iter.next()
          if (result.isEmpty) {
            result = Some(next)
          } else {
            result = Some(result.get.append(next))
          }
        }

        if (result.isDefined) {
          result.get match {
            case node: XdmNode =>
              if (Option(node.getBaseURI).isDefined) {
                val prop = mutable.HashMap.empty[QName, XdmValue]
                prop.put(XProcConstants._base_uri, new XdmAtomicValue(node.getBaseURI))
                consume(result.get, "result", prop.toMap, primaryOutputProperties)
              } else {
                consume(result.get, "result", Map(), primaryOutputProperties)
              }
            case _ =>
              consume(result.get, "result", Map(), primaryOutputProperties)
          }
        }

      case xdm: XdmDestination =>
        val tree = xdm.getXdmNode
        if (Option(tree.getBaseURI).isDefined) {
          val prop = mutable.HashMap.empty[QName, XdmValue]
          prop.put(XProcConstants._base_uri, new XdmAtomicValue(tree.getBaseURI))
          consume(tree, "result", prop.toMap, primaryOutputProperties)
        } else {
          consume(tree, "result", Map(), primaryOutputProperties)
        }
    }

    for ((uri, destination) <- secondaryResults) {
      val serprops = secondaryOutputProperties(uri)
      destination match {
        case raw: RawDestination =>
          val iter = raw.getXdmValue.iterator()
          while (iter.hasNext) {
            val next = iter.next()
            consumeSecondary(next, uri, serprops)
          }
        case xdm: XdmDestination =>
          val tree = xdm.getXdmNode
          consumeSecondary(tree, uri, serprops)
      }
    }
  }

  private def consumeSecondary(item: XdmItem, uri: URI, serprops: Map[QName,XdmValue]): Unit = {
    val prop = mutable.HashMap.empty[QName, XdmValue]
    prop.put(XProcConstants._base_uri, new XdmAtomicValue(uri))

    // Sigh. Secondary output documents don't have the correct intrinsict base URI,
    // so we rebuild documents around them where we can with the correct URI.

    item match {
      case node: XdmNode =>
        val rebuild = new SaxonTreeBuilder(this.config)
        rebuild.startDocument(uri)
        rebuild.addSubtree(node)
        rebuild.endDocument()
        consume(rebuild.result, "secondary", prop.toMap, serprops)
      case _ =>
        consume(item, "secondary", prop.toMap, serprops)
    }
  }

  override def reset(): Unit = {
    super.reset()
    goesBang = None
  }

  private class MyDestination(map: mutable.HashMap[QName,XdmValue]) extends RawDestination {
    private var destination = Option.empty[Destination]
    private var destBase = Option.empty[URI]

    override def setDestinationBaseURI(baseURI: URI): Unit = {
      destBase = Some(baseURI)
      if (destination.isDefined) {
        destination.get.setDestinationBaseURI(baseURI)
      }
    }

    override def getDestinationBaseURI: URI = destBase.orNull

    override def getReceiver(pipe: PipelineConfiguration, params: SerializationProperties): Receiver = {
      val tree = Option(params.getProperty(SaxonOutputKeys.BUILD_TREE))

      val props = params.getProperties
      val enum = props.propertyNames()
      while (enum.hasMoreElements) {
        val name: String = enum.nextElement().asInstanceOf[String]
        val qname = if (name.startsWith("{")) {
          ValueParser.parseClarkName(name)
        } else {
          new QName(name)
        }
        val value = props.get(name).asInstanceOf[String]
        if (value == "yes" || value == "no") {
          map.put(qname, new XdmAtomicValue(value == "yes"))
        } else {
          map.put(qname, new XdmAtomicValue(value))
        }
      }

      val dest = if (tree.getOrElse("yes") == "yes") {
        new XdmDestination()
      } else {
        new RawDestination()
      }

      if (destBase.isDefined) {
        dest.setDestinationBaseURI(destBase.get)
      }

      destination = Some(dest)
      primaryDestination = dest
      dest.getReceiver(pipe, params)
    }

    override def closeAndNotify(): Unit = {
      if (destination.isDefined) {
        destination.get.closeAndNotify()
      }
    }

    override def close(): Unit = {
      if (destination.isDefined) {
        destination.get.close()
      }
    }
  }

  private class DocumentHandler extends Function[URI, Destination] {
    override def apply(uri: URI): Destination = {
      val xdmResult: XdmDestination = new XdmDestination
      xdmResult.setBaseURI(uri)
      xdmResult.onClose(new DocumentCloseAction(uri, xdmResult))
      xdmResult
    }
  }

  private class BaseURIMapper(val origBase: String) extends Function[NodeInfo, String] {
    override def apply(node: NodeInfo): String = {
      var base = node.getBaseURI
      if (Option(origBase).isDefined && (Option(base).isEmpty || base == "")) {
        base = origBase
      }
      base
    }
  }

  private class SystemIdMapper extends Function[NodeInfo, String] {
    // This is a nop for now
    override def apply(node: NodeInfo): String = {
      node.getSystemId
    }
  }

  private class DocumentCloseAction(val uri: URI, destination: XdmDestination) extends Action {
    override def act(): Unit = {
      var doc = destination.getXdmNode
      val bmapper = new BaseURIMapper(doc.getBaseURI.toASCIIString)
      val smapper = new SystemIdMapper()
      val treeinfo = doc.getUnderlyingNode.getTreeInfo
      val rebaser = new RebasedDocument(treeinfo, bmapper.asJava, smapper.asJava)
      val xfixbase = rebaser.wrap(doc.getUnderlyingNode)
      doc = new XdmNode(xfixbase)

      // FIXME: what should the properties be?
      consume(doc, "secondary", Map(), Map())
    }
  }

  class MyResultDocumentResolver(val sconfig: Configuration) extends ResultDocumentResolver() {
    override def resolve(context: XPathContext, href: String, baseUri: String, properties: SerializationProperties): Receiver = {
      val tree = Option(properties.getProperty(SaxonOutputKeys.BUILD_TREE))
      val uri = ResolveURI.makeAbsolute(href, baseUri)
      val destination = if (tree.getOrElse("no") == "yes") {
        new XdmDestination()
      } else {
        new RawDestination()
      }

      val xprocProps = mutable.HashMap.empty[QName, XdmValue]
      for (rawkey <- properties.getProperties.keySet().asScala) {
        val key = rawkey.toString
      }

      secondaryOutputProperties.put(uri, xprocProps.toMap)
      secondaryResults.put(uri, destination)

      val pc = new PipelineConfiguration(sconfig)
      destination.getReceiver(pc, properties);
    }
  }

  private class CatchMessages extends MessageListener {
    override def message(content: XdmNode, terminate: Boolean, locator: SourceLocator): Unit = {
      val treeWriter = new SaxonTreeBuilder(config)
      treeWriter.startDocument(content.getBaseURI)
      treeWriter.addStartElement(XProcConstants.c_error)
      treeWriter.addSubtree(content)
      treeWriter.addEndElement()
      treeWriter.endDocument()

      // FIXME: step.reportError(treeWriter.getResult());
      // FIXME: step.info(step.getNode(), content.toString());
    }
  }

  private class MyErrorListener(val compileTime: Boolean) extends ErrorListener {
    override def warning(e: TransformerException): Unit = ()

    override def error(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcXsltCompileError(e.getMessage, e, location))
    }

    override def fatalError(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcXsltCompileError(e.getMessage, e, location))
    }
  }
}
