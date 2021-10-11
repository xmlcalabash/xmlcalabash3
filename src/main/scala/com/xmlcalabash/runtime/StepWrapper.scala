package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.{StepSignature, XMLCalabashConfig}
import net.sf.saxon.s9api.{QName, XdmValue}

class StepWrapper(protected[xmlcalabash] val step: XmlStep, val signature: StepSignature) extends StepExecutable {
  override def inputSpec: XmlPortSpecification = step.inputSpec
  override def outputSpec: XmlPortSpecification = step.outputSpec
  override def bindingSpec: BindingSpecification = step.bindingSpec
  override def setConsumer(consumer: XProcDataConsumer): Unit = step.setConsumer(consumer)
  override def setLocation(location: Location): Unit = step.setLocation(location)

  override def receiveBinding(variable: NameValueBinding): Unit = {
    step.receiveBinding(variable)
  }
  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    step.receive(port,item,metadata)
  }
  override def configure(config: XMLCalabashConfig, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    step.configure(config, stepType, stepName, params)
  }
  override def initialize(config: RuntimeConfiguration): Unit = {
    step.initialize(config)
  }
  override def run(context: StaticContext): Unit = {
    step.run(context)
  }
  override def reset(): Unit = step.reset()
  override def abort(): Unit = step.abort()
  override def stop(): Unit = step.stop()

  override def toString: String = {
    s"StepWrapper for $step"
  }
}
