package com.xmlcalabash.config

import com.jafpl.graph.Graph
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.XMLCalabashDebugOptions.{GRAPH, OPENGRAPH, PIPELINE, TREE}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xxml.XDeclareStep
import com.xmlcalabash.util.PipelineEnvironmentOption
import net.sf.saxon.s9api.{QName, XdmDestination, XdmNode}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.InputSource

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileOutputStream, PrintWriter}
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object XMLCalabashDebugOptions {
  val TREE = "tree"
  val GRAPH = "graph"
  val OPENGRAPH = "open-graph"
  val PIPELINE = "pipeline"
}

class XMLCalabashDebugOptions(config: XMLCalabash) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val _injectables = ListBuffer.empty[String]

  private val dumped = mutable.HashMap.empty[XDeclareStep, mutable.HashSet[String]]
  private val dumpCount = mutable.HashMap.empty[XDeclareStep, mutable.HashMap[String, Long]]

  def injectables: List[String] = _injectables.toList

  def injectables_=(list: List[String]): Unit = {
    _injectables.clear()
    _injectables ++= list
  }

  def environmentOptions(name: QName): List[PipelineEnvironmentOption] = {
    config.parameters collect { case p: PipelineEnvironmentOption => p } filter { _.eqname == name.getEQName }
  }

  def graphviz_dot: Option[String] = {
    var dot = Option.empty[String]
    for (option <- environmentOptions(XProcConstants.cc_graphviz)) {
      if (dot.isEmpty && option.getString.isDefined) {
        for (path <- option.getString.get.split("\\s+")) {
          val exec = new File(path)
          if (exec.exists() && exec.canExecute) {
            dot = Some(path)
          }
        }
      }
    }
    dot
  }

  def run: Boolean = {
    val envOpt = environmentOptions(XProcConstants.cc_run).headOption
    if (envOpt.isDefined && envOpt.head.getBoolean.isDefined) {
      envOpt.head.getBoolean.get
    } else {
      true
    }
  }

  def stacktrace: Boolean = {
    val envOpt = environmentOptions(XProcConstants.cc_stacktrace).headOption
    if (envOpt.isDefined && envOpt.head.getBoolean.isDefined) {
      envOpt.head.getBoolean.get
    } else {
      false
    }
  }

  def logLevel: Option[String] = {
    val envOpt = environmentOptions(XProcConstants.cc_loglevel).headOption
    if (envOpt.isDefined && envOpt.head.getString.isDefined) {
      envOpt.head.getString
    } else {
      None
    }
  }

  def outputDirectory: String = {
    val envOpt = environmentOptions(XProcConstants.cc_debug_output_directory).headOption
    if (envOpt.isDefined && envOpt.head.getString.isDefined) {
      val dir = envOpt.head.getString.get
      val fs = System.getProperty("file.separator", "/")
      if (dir.endsWith(fs)) {
        dir.substring(0, dir.length - 1)
      } else {
        dir
      }
    } else {
      "."
    }
  }

  def tree: Boolean = graph_option(TREE)
  def pipeline: Boolean = graph_option(PIPELINE)
  def graph: Boolean = graph_option(GRAPH)
  def open_graph: Boolean = graph_option(OPENGRAPH)

  private def graph_option(name: String): Boolean = {
    val found = environmentOptions(XProcConstants.cc_graph) find {
      _.getString.get == name
    }
    found.isDefined
  }

  // ===========================================================================================

  def dumpTree(decl: XDeclareStep): Unit = {
    dump(decl, TREE)
  }

  def dumpPipeline(decl: XDeclareStep): Unit = {
    dump(decl, PIPELINE)
  }

  def dumpGraph(decl: XDeclareStep, graph: Graph): Unit = {
    dump(decl, GRAPH, Some(graph))
  }

  def dumpOpenGraph(decl: XDeclareStep, graph: Graph): Unit = {
    dump(decl, OPENGRAPH, Some(graph))
  }

  private def dump(decl: XDeclareStep, opt: String): Unit = {
    dump(decl, opt, None)
  }

  private def dump(decl: XDeclareStep, opt: String, graph: Option[Graph]): Unit = {
    if (!graph_option(opt)) {
      return
    }

    val output = dumped.getOrElse(decl, mutable.HashSet.empty[String])
    if (output.contains(opt)) {
      return
    }
    output += opt
    dumped.put(decl, output)

    val name = if (decl.stepName.startsWith("!")) {
      decl.stepName.substring(1).replace(".", "_")
    } else {
      decl.stepName
    }

    val counts = dumpCount.getOrElse(decl, mutable.HashMap.empty[String, Long])
    val count = counts.getOrElse(opt, 0L)

    var ext = ""
    if (count > 0) {
      ext = s"$count%03d"
    }

    counts.put(opt, count + 1)

    opt match {
      case TREE =>
        val basefn = name
        val fn = s"$outputDirectory/$basefn$ext.xml"
        val fos = new FileOutputStream(new File(fn))
        val pw = new PrintWriter(fos)
        pw.write(decl.dump.toString)
        pw.close()
        fos.close()
      case PIPELINE =>
        val basefn = name
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        pw.write(decl.dump.toString)
        pw.close()
        //println("********* PIPELINE ************")
        //println(baos.toString(StandardCharsets.UTF_8))
        svgPipeline(fn, baos)
      case GRAPH =>
        val basefn = s"${name}_graph"
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        //println("********* GRAPH ************")
        //println(graph.get.asXML.toString)
        pw.write(graph.get.asXML.toString)
        pw.close()
        svgGraph(fn, baos, "pgx2dot.xsl")
      case OPENGRAPH =>
        val basefn = name
        ext = s"_open$ext"
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        pw.write(graph.get.asXML.toString)
        pw.close()
        //println("********* OPEN GRAPH ************")
        //println(baos.toString(StandardCharsets.UTF_8))
        svgGraph(fn, baos, "pg2dot.xsl")
    }
  }

  private def svgGraph(fn: String, xmlBaos: ByteArrayOutputStream, style: String): Unit = {
    if (config.debugOptions.graphviz_dot.isEmpty) {
      logger.error(s"GraphViz dot not configured, cannot dump $fn.")
      return
    }

    // Get the source node
    val bais = new ByteArrayInputStream(xmlBaos.toByteArray)
    val builder = config.processor.newDocumentBuilder()
    val graphdoc = builder.build(new SAXSource(new InputSource(bais)))

    graphToSvg(s"/com/jafpl/stylesheets/${style}", graphdoc, fn)
  }

  private def svgPipeline(fn: String, xmlBaos: ByteArrayOutputStream): Unit = {
    if (config.debugOptions.graphviz_dot.isEmpty) {
      logger.error(s"GraphViz dot not configured, cannot dump $fn.")
      return
    }

    val processor = config.processor

    // Get the source node
    val bais = new ByteArrayInputStream(xmlBaos.toByteArray)
    val builder = processor.newDocumentBuilder()
    val graphdoc = builder.build(new SAXSource(new InputSource(bais)))

    // Get the first stylesheet
    val stylesheet = getClass.getResourceAsStream("/com/xmlcalabash/stylesheets/pl2dot.xsl")
    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    val exec = compiler.compile(new SAXSource(new InputSource(stylesheet)))

    // Transform to dot: XML
    val transformer = exec.load()
    transformer.setInitialContextNode(graphdoc)
    val result = new XdmDestination()
    transformer.setDestination(result)
    transformer.transform()
    val xformed = result.getXdmNode

    graphToSvg("/com/xmlcalabash/stylesheets/dot2txt.xsl", xformed, fn)
  }

  private def graphToSvg(resourceName: String, xmldot: XdmNode, outputFn: String): Unit = {
    val processor = config.processor

    // Get the stylesheet
    val stylesheet = getClass.getResourceAsStream(resourceName)
    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    val exec = compiler.compile(new SAXSource(new InputSource(stylesheet)))

    // Transform to dot
    val transformer = exec.load()
    transformer.setInitialContextNode(xmldot)
    val result = new XdmDestination()
    transformer.setDestination(result)
    transformer.transform()
    val xformed = result.getXdmNode

    // Write the DOT file to a temp file
    val temp = File.createTempFile("calabash", ".dot")
    temp.deleteOnExit()
    val dot = new FileOutputStream(temp)
    val pw = new PrintWriter(dot)
    pw.println(xformed.getStringValue)
    pw.close()

    // Transform it into SVG
    val rt = Runtime.getRuntime
    val args = Array(graphviz_dot.get, "-Tsvg", temp.getAbsolutePath, "-o", outputFn)
    val p = rt.exec(args)
    p.waitFor()

    temp.delete()
  }
}
