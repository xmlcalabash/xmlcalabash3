package com.xmlcalabash.steps.text

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.xc.XsltStylesheet

import javax.xml.transform.{ErrorListener, TransformerException}
import net.sf.saxon.s9api.{QName, SaxonApiException, XdmDestination, XdmValue}
import net.sf.saxon.trans.XPathException

class Sort() extends TextLines {
  private val _sort_key = new QName("", "sort-key")
  private val _order = new QName("", "order")
  private val _case_order = new QName("", "case-order")
  private val _lang = new QName("", "lang")
  private val _collation = new QName("", "collation")
  private val _stable = new QName("", "stable")

  private val _sort = new QName("", "sort")
  private val _line = new QName("", "line")

  private val elistener = new SortErrorListener()
  private var goesBang = Option.empty[XProcException]
  private var keyNamespaceBindings = Map.empty[String,String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def receiveBinding(variable: NameValueBinding): Unit = {
    super.receiveBinding(variable)
    if (variable.name == _sort_key) {
      keyNamespaceBindings = variable.context.nsBindings
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val xslbuilder = new XsltStylesheet(config, context.nsBindings, List(), "2.0")

    xslbuilder.startVariable("lines", "element()*")
    for (line <- lines) {
      xslbuilder.literal(_line, line)
    }
    xslbuilder.endVariable()

    xslbuilder.startNamedTemplate("sort")
    xslbuilder.startForEach("$lines")

    val sort_key = optionalStringBinding(_sort_key)
    val order = optionalStringBinding(_order)
    val case_order = optionalStringBinding(_case_order)
    val lang = optionalStringBinding(_lang)
    val collation = optionalStringBinding(_collation)
    val stable = optionalStringBinding(_stable)

    if (order.isDefined && (order.get != "ascending" && order.get != "descending")) {
      throw XProcException.xdBadValue(order.get, location)
    }

    if (case_order.isDefined && (case_order.get != "upper-first" && case_order.get != "lower-first")) {
      throw XProcException.xdBadValue(case_order.get, location)
    }

    xslbuilder.startSort(sort_key.getOrElse("."), keyNamespaceBindings, lang, order, collation, stable, case_order)

    xslbuilder.endSort()
    xslbuilder.valueOf(".")
    xslbuilder.text("\n")
    xslbuilder.endForEach()
    xslbuilder.endTemplate()

    val stylesheet = xslbuilder.endStylesheet()

    val processor = config.processor
    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    compiler.setErrorListener(elistener)

    val exec = try {
      compiler.compile(stylesheet.asSource())
    } catch {
      case ex: SaxonApiException =>
        if (goesBang.isDefined) {
          throw goesBang.get
        } else {
          throw XProcException.xdStepFailed(ex.getMessage, location)
        }
    }

    val transformer = exec.load()
    transformer.setInitialTemplate(_sort)
    transformer.setErrorListener(elistener)

    val result = new XdmDestination()
    transformer.setDestination(result)
    try {
      transformer.transform()
    } catch {
      case ex: SaxonApiException =>
        if (goesBang.isDefined) {
          throw goesBang.get
        } else {
          throw XProcException.xdStepFailed(ex.getMessage, location)
        }
    }

    val xformed = result.getXdmNode

    consumer.get.receive("result", xformed, new XProcMetadata(MediaType.TEXT))
  }

  override def reset(): Unit = {
    super.reset()
    goesBang = None
  }

  private class SortErrorListener() extends ErrorListener {
    override def warning(e: TransformerException): Unit = ()

    override def error(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcSortError(e.getMessage, location))
    }

    override def fatalError(e: TransformerException): Unit = {
      e match {
        case xpe: XPathException =>
          if (xpe.getErrorCodeQName == XProcException.xtte(1020)) {
            goesBang = Some(XProcException.xcSortKeyError(location))
          } else {
            // ??? This error is for problems applying the sort-key to a line.
            // It works for that, but I can't be sure there aren't other possible errors.
            goesBang = Some(XProcException.xcSortError(e.getMessage, location))
          }
        case _ =>
          ()
      }

      if (goesBang.isEmpty) {
        goesBang = Some(XProcException.xdStepFailed(e.getMessage, location))
      }
    }
  }
}
