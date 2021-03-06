package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.util.MinimalStaticContext
import net.sf.saxon.s9api.{QName, XdmValue}

trait XmlStep {
  def inputSpec: XmlPortSpecification
  def outputSpec: XmlPortSpecification
  def bindingSpec: BindingSpecification
  def setConsumer(consumer: XProcDataConsumer): Unit
  def setLocation(location: Location): Unit
  def receiveBinding(variable: NameValueBinding): Unit
  def receive(port: String, item: Any, metadata: XProcMetadata): Unit
  def configure(config: XMLCalabash, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit
  def initialize(config: RuntimeConfiguration): Unit
  def run(context: MinimalStaticContext): Unit
  def reset(): Unit
  def abort(): Unit
  def stop(): Unit
}
