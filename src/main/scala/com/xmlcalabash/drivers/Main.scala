package com.xmlcalabash.drivers

import com.jafpl.exceptions.{JafplException, JafplLoopDetected}
import com.jafpl.graph.{Binding, Node}
import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.exceptions.{ModelException, ParseException, XProcException}
import com.xmlcalabash.model.xml.{DeclareStep, Parser}
import com.xmlcalabash.runtime.{PrintingConsumer, StaticContext, XProcMetadata}
import com.xmlcalabash.util.{ArgBundle, DefaultErrorExplanation, URIUtils}
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URI

object Main extends App {
  type OptionMap = Map[Symbol, Any]

  var config: XMLCalabashConfig = _
  var options: ArgBundle = _
  var decl = Option.empty[DeclareStep]
  var errored = false

  try {
    config = XMLCalabashConfig.newInstance()
    options = new ArgBundle(config, args.toList)

    if (options.hasPipeline) {
      runPipeline()
    } else {
      if (options.help) {
        longHelp()
      } else {
        shortHelp()
      }
    }
  } catch {
    case ex: Exception =>
      errored = true

      val explain = if (Option(config).isDefined && Option(config.errorExplanation).isDefined) {
        config.errorExplanation
      } else {
        new DefaultErrorExplanation()
      }

      if (Option(config).isDefined) {
        config.debugOptions.dumpStacktrace(decl, ex)
      }

      val mappedex = XProcException.mapPipelineException(ex)

      mappedex match {
        case model: ModelException =>
          println(model)
        case parse: ParseException =>
          println(parse)
        case jafpl: JafplLoopDetected =>
          println("Loop detected:")
          var first = true
          var pnode = Option.empty[Node]
          for (node <- jafpl.nodes) {
            val prefix = if (first) {
              "An output from"
            } else {
              "flows to"
            }

            node match {
              case bind: Binding =>
                val bnode = bind.bindingFor
                pnode = Some(bnode)

                val bnodeLabel = bnode.userLabel.getOrElse(bnode.label)
                val bindLabel = bind.userLabel.getOrElse(bind.label)

                println(s"\tis the context item for $bnodeLabel/$bindLabel; $bnodeLabel")
                if (bnode.location.isDefined) {
                  println(s"\t\t${bnode.location.get}")
                }
              case _ =>
                if (pnode.isEmpty || pnode.get != node) {
                  println(s"\t$prefix ${node.userLabel.getOrElse(node.label)}")
                  if (node.location.isDefined) {
                    println(s"\t\t${node.location.get}")
                  }
                  pnode = Some(node)
                }
            }

            first = false
          }
        case jafpl: JafplException =>
          println(jafpl.getMessage())
        case xproc: XProcException =>
          val code = xproc.code
          val message = if (xproc.message.isDefined) {
            xproc.message.get
          } else {
            code match {
              case qname: QName =>
                explain.message(qname, xproc.variant, xproc.details)
              case _ =>
                s"Configuration error: code ($code) is not a QName"
            }
          }
          if (xproc.location.isDefined) {
            println(s"ERROR ${xproc.location.get} $code $message")
          } else {
            println(s"ERROR $code $message")
          }

          if (Option(options).isDefined && options.verbose && code.isInstanceOf[QName]) {
            val explanation = config.errorExplanation.explanation(code, xproc.variant)
            if (explanation != "") {
              println(explanation)
            }
          }

        case _ =>
          println("Error: " + ex)
      }
  }

  if (errored) {
    System.exit(1)
  }

  def shortHelp(): Unit = {
    help(getClass.getResourceAsStream("/usage-short.txt"))
  }

  def longHelp(): Unit = {
    help(getClass.getResourceAsStream("/usage-long.txt"))
  }

  private def help(stream: InputStream): Unit = {
    if (stream == null) {
      throw XProcException.xiThisCantHappen("help text is missing.", None)
    }
    val reader = new BufferedReader(new InputStreamReader(stream))
    var line = reader.readLine()
    while (Option(line).isDefined) {
      println(line)
      line = reader.readLine()
    }
  }

  def runPipeline(): Unit = {
    if (config.debugOptions.logLevel.isDefined) {
      // Try to tweak the log level. This will only work if the user hasn't
      // reconfigured logging. But if they've reconfigured logging, presumably
      // they don't need this!
      val level = config.debugOptions.logLevel.get
      val logcontext = LoggerFactory.getILoggerFactory
      val logger = logcontext.getLogger("root")
      logger match {
        case lgr: ch.qos.logback.classic.Logger =>
          lgr.setLevel(ch.qos.logback.classic.Level.toLevel(level))
        case _ =>
          logger.warn(s"Logging configuration doesn't support command line --${level} option")
      }
    }

    config.debugOptions.injectables = options.injectables

    val parser = new Parser(config)
    val pipeline = parser.loadDeclareStep(new URI(options.pipeline))

    decl = Some(pipeline)

    config.debugOptions.dumpTree(pipeline)
    config.debugOptions.dumpPipeline(pipeline)

    val runtime = pipeline.runtime()

    if (!config.debugOptions.run) {
      System.exit(0)
    }

    for (port <- options.inputs.keySet) {
      for (filename <- options.inputs(port)) {
        val href = URIUtils.cwdAsURI.resolve(filename)
        val request = new DocumentRequest(href)
        val response = config.documentManager.parse(request)
        runtime.input(port, response.value, new XProcMetadata(response.contentType))
      }
    }

    for (port <- runtime.outputs) {
      val output = runtime.decl.output(port)
      val pc = if (options.outputs.contains(port)) {
        new PrintingConsumer(runtime, output, options.outputs(port))
      } else {
        new PrintingConsumer(runtime, output)
      }
      runtime.output(port, pc)
    }

    for ((name, value) <- options.options) {
      runtime.option(name, value.value, new StaticContext(config))
    }

    runtime.run()
  }

}
