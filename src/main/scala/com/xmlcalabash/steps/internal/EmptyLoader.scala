package com.xmlcalabash.steps.internal

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.runtime.params.EmptyLoaderParams
import com.xmlcalabash.runtime.{ImplParams, StaticContext, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.QName

class EmptyLoader() extends AbstractLoader {
  override def inputSpec: XmlPortSpecification = {
    XmlPortSpecification.NONE
  }
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def configure(config: XMLCalabashConfig, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    if (params.isEmpty) {
      throw new RuntimeException("empty loader params required")
    }

    params.get match {
      case doc: EmptyLoaderParams =>
        exprContext = doc.context
      case _ =>
        throw new RuntimeException("document loader params wrong type")
    }
  }

  override def runningMessage(): Unit = {
    if (DefaultXmlStep.showRunningMessage) {
      logger.info("Loading empty")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)
    // Produce nothing.
  }
}
