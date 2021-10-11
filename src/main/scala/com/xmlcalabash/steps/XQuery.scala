package com.xmlcalabash.steps

import java.io.ByteArrayOutputStream
import java.net.URI
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, XProcCollectionFinder}

import javax.xml.transform.{ErrorListener, TransformerException}
import net.sf.saxon.event.{PipelineConfiguration, Receiver}
import net.sf.saxon.lib.SaxonOutputKeys
import net.sf.saxon.s9api.{Destination, QName, RawDestination, Serializer, ValidationMode, XdmArray, XdmAtomicValue, XdmDestination, XdmFunctionItem, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.serialize.SerializationProperties

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XQuery extends DefaultXmlStep {
  private var query = Option.empty[XdmNode]
  private var queryMetadata = Option.empty[XProcMetadata]
  private val defaultCollection = ListBuffer.empty[XdmNode]
  private val inputSequence = ListBuffer.empty[XdmItem]
  private val inputMetadata = ListBuffer.empty[XProcMetadata]

  private var parameters = Map.empty[QName, XdmValue]
  private var version = Option.empty[String]

  private var goesBang = Option.empty[XProcException]
  private val outputProperties = mutable.HashMap.empty[QName, XdmValue]

  private var primaryDestination: Destination = _

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ZERO_OR_MORE, "query" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/octet-stream"), "query" -> List("application/xml", "text/plain"))
  )

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    port match {
      case "source" =>
        item match {
          case node: XdmNode =>
            defaultCollection += node
            inputSequence += node
            inputMetadata += metadata
          case item: XdmItem =>
            inputSequence += item
            inputMetadata += metadata
        }
      case "query" =>
        query = Some(item.asInstanceOf[XdmNode])
        queryMetadata = Some(metadata)
      case _ => ()
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    super.run(staticContext)

    val pmap = mapBinding(XProcConstants._parameters)
    if (pmap.size() > 0) {
      parameters = ValueParser.parseParameters(pmap, staticContext)
    }

    version = optionalStringBinding(XProcConstants._version)

    version.getOrElse("3.1") match {
      case "3.0" => xquery30()
      case "3.1" => xquery31()
      case _ =>
        throw XProcException.xcXQueryVersionNotAvailable(version.getOrElse(""), location)
    }
  }

  private def xquery30(): Unit = {
    for (meta <- inputMetadata) {
      val ctype = meta.contentType
      if (!ctype.xmlContentType && !ctype.htmlContentType && !ctype.textContentType) {
        throw XProcException.xcXQueryInputNot30Compatible(ctype, location)
      }
    }

    for ((name, value) <- parameters) {
      value match {
        case _: XdmAtomicValue => ()
        case _: XdmNode => ()
        case _: XdmMap =>
          throw XProcException.xcXQueryInvalidParameterType(name, "map", location)
        case _: XdmArray => ()
          throw XProcException.xcXQueryInvalidParameterType(name, "array", location)
        case _: XdmFunctionItem => ()
          throw XProcException.xcXQueryInvalidParameterType(name, "function", location)
        case _ =>
          logger.debug(s"Unexpected parameter type: ${value} passed to p:xquery")
      }
    }
  }

  private def xquery31(): Unit = {
    runXQueryProcessor()
  }

  private def runXQueryProcessor(): Unit = {
    val document: Option[XdmItem] = inputSequence.headOption

    val runtime = this.config.config
    val processor = runtime.processor
    val underlyingConfig = processor.getUnderlyingConfiguration
    // FIXME: runtime.getConfigurer().getSaxonConfigurer().configXQuery(config);

    val collectionFinder = underlyingConfig.getCollectionFinder
    val unparsedTextURIResolver = underlyingConfig.getUnparsedTextURIResolver

    underlyingConfig.setDefaultCollection(XProcCollectionFinder.DEFAULT)
    underlyingConfig.setCollectionFinder(new XProcCollectionFinder(runtime, defaultCollection.toList, collectionFinder))

    val compiler = processor.newXQueryCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    compiler.setErrorListener(new MyErrorListener(true))
    val exec = try {
      var xquery = query.get.getStringValue
      if (queryMetadata.get.contentType.xmlContentType) {
        val root = S9Api.documentElement(query.get)
        if (root.get.getNodeName != XProcConstants.c_query) {
          val baos = new ByteArrayOutputStream()
          val serializer = config.processor.newSerializer(baos)
          serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8")
          serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "true")
          S9Api.serialize(config.config, List(query.get), serializer)
          xquery = baos.toString("utf-8")
        }
      }

      compiler.compile(xquery)
    } catch {
      case e: Exception =>
        throw goesBang.getOrElse(e)
    }
    val queryEval = exec.load()

    for ((param, value) <- parameters) {
      queryEval.setExternalVariable(param, value)
    }

    if (document.isDefined) {
      queryEval.setContextItem(document.get)
    }

    val result = new MyDestination(outputProperties)
    queryEval.setDestination(result)

    queryEval.setSchemaValidationMode(ValidationMode.DEFAULT)
    queryEval.setErrorListener(new MyErrorListener(false))

    try {
      queryEval.run()
    } catch {
      case ex: Throwable =>
        throw XProcException.xcXQueryEvalError(ex.getMessage, location)
    }

    try {
      val iter = queryEval.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        consume(item, "result", Map(), outputProperties.toMap)
        /*
        if (item.isAtomicValue) {
          consumer.get.receive("result", item, new XProcMetadata(MediaType.JSON))
        } else {
          val node = item.asInstanceOf[XdmNode]
          val builder = new SaxonTreeBuilder(config)
          builder.startDocument(None)
          builder.addSubtree(node)
          builder.endDocument()

          if (node.getNodeKind == XdmNodeKind.TEXT) {
            consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.TEXT))
          } else {
            consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
          }
        }

         */
      }
    } catch {
      case e: Exception =>
        throw goesBang.getOrElse(e)
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

      map.addAll(S9Api.serializationPropertyMap(params))

      val dest = if (tree.getOrElse("no") == "yes") {
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

  private class MyErrorListener(val compileTime: Boolean) extends ErrorListener {
    override def warning(e: TransformerException): Unit = ()

    override def error(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcXsltCompileError(e.getMessage, e, location))
    }

    override def fatalError(e: TransformerException): Unit = {
      if (compileTime) {
        goesBang = Some(XProcException.xcXQueryCompileError(e.getMessage, location))
      } else {
        goesBang = Some(XProcException.xcXQueryEvalError(e.getMessage, location))
      }
    }
  }
}
