package com.xmlcalabash.config

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileOutputStream, PrintStream, PrintWriter}
import com.jafpl.graph.Graph
import com.xmlcalabash.model.xml.DeclareStep
import com.xmlcalabash.model.util.XProcConstants

import javax.xml.transform.sax.SAXSource
import net.sf.saxon.s9api.{QName, XdmDestination, XdmNode}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.InputSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

protected class XMLCalabashDebugOptions(config: XMLCalabashConfig) {
  private val TREE = "tree"
  private val PIPELINE = "pipeline"
  private val GRAPH = "graph"
  private val OPENGRAPH = "open-graph"

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val _injectables = ListBuffer.empty[String]
  private var _graphviz_dot = Option.empty[String]
  private var _run = true

  private var _stacktrace = false
  private var _output_directory = "."
  private var _tree = Option.empty[String]
  private var _pipeline = Option.empty[String]
  private var _graph = Option.empty[String]
  private var _open_graph = Option.empty[String]

  private val debugOptions = mutable.HashSet.empty[String]
  private val dumped = mutable.HashMap.empty[DeclareStep, mutable.HashSet[String]]
  private val dumpCount = mutable.HashMap.empty[DeclareStep, mutable.HashMap[String, Long]]

  private var _logLevel = Option.empty[String]

  def injectables: List[String] = _injectables.toList

  def injectables_=(list: List[String]): Unit = {
    _injectables.clear()
    _injectables ++= list
  }

  def graphviz_dot: Option[String] = _graphviz_dot

  def graphviz_dot_=(dot: String): Unit = {
    _graphviz_dot = Some(dot)
  }

  def run: Boolean = _run

  def run_=(run: Boolean): Unit = {
    _run = run
  }

  def stacktrace: Boolean = _stacktrace
  def stacktrace_=(trace: Boolean): Unit = {
    _stacktrace = trace
  }

  def logLevel: Option[String] = _logLevel
  def logLevel_=(level: String): Unit = {
    _logLevel = Some(level)
  }

  def outputDirectory: String = _output_directory

  def outputDirectory_=(dir: String): Unit = {
    _output_directory = dir
  }

  def tree: Option[String] = _tree
  def tree_=(tree: Option[String]): Unit = {
    _tree = tree
    debugOptions += TREE
  }

  def pipeline: Option[String] = _pipeline
  def pipeline_=(graph: Option[String]): Unit = {
    _pipeline = graph
    debugOptions += PIPELINE
  }

  def graph: Option[String] = _graph
  def graph_=(graph: Option[String]): Unit = {
    _graph = graph
    debugOptions += GRAPH
  }

  def openGraph: Option[String] = _open_graph

  def openGraph_=(graph: Option[String]): Unit = {
    _open_graph = graph
    debugOptions += OPENGRAPH
  }

  // ===========================================================================================

  def dumpStacktrace(decl: Option[DeclareStep], ex: Exception): Unit = {
    if (stacktrace) {
      ex.printStackTrace(System.err)
    }
  }

  def dumpTree(decl: DeclareStep): Unit = {
    dump(decl, TREE)
  }

  def dumpPipeline(decl: DeclareStep): Unit = {
    dump(decl, PIPELINE)
  }

  def dumpGraph(decl: DeclareStep, graph: Graph): Unit = {
    dump(decl, GRAPH, Some(graph))
  }

  def dumpOpenGraph(decl: DeclareStep, graph: Graph): Unit = {
    dump(decl, OPENGRAPH, Some(graph))
  }

  private def dump(decl: DeclareStep, opt: String): Unit = {
    dump(decl, opt, None)
  }

  private def dump(decl: DeclareStep, opt: String, graph: Option[Graph]): Unit = {
    if (!debugOptions.contains(opt)) {
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
        val basefn = tree.getOrElse(name)
        val fn = s"$outputDirectory/$basefn$ext.xml"
        val fos = new FileOutputStream(new File(fn))
        val pw = new PrintWriter(fos)
        pw.write(decl.xdump.toString)
        pw.close()
        fos.close()
      case PIPELINE =>
        val basefn = graph.getOrElse(name)
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        pw.write(decl.xdump.toString)
        pw.close()
        svgPipeline(fn, baos)
      case GRAPH =>
        val basefn = graph.getOrElse(name)
        if (debugOptions.contains(PIPELINE) && graph.isEmpty
          || debugOptions.contains(OPENGRAPH) && openGraph.isEmpty) {
          ext = s"_graph$ext"
        }
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        pw.write(graph.get.asXML.toString)
        pw.close()
        svgGraph(fn, baos, "pgx2dot.xsl")
      case OPENGRAPH =>
        val basefn = openGraph.getOrElse(name)
        ext = s"_open$ext"
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        pw.write(graph.get.asXML.toString)
        pw.close()
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
