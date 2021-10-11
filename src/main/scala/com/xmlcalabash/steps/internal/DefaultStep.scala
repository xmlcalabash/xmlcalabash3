package com.xmlcalabash.steps.internal

import com.jafpl.graph.Location
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, Step}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XmlPortSpecification}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultStep extends Step {
  private var _location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected var config: Option[XMLCalabashRuntime] = None
  protected val bindings = mutable.HashMap.empty[String,Message]

  // ==========================================================================

  override def location: Option[Location] = _location
  override def location_=(location: Location): Unit = {
    _location = Some(location)
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    bindings.put(bindmsg.name, bindmsg.message)
  }

  override def setConsumer(consumer: DataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def consume(port: String, message: Message): Unit = {
    // nop
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case runtime: XMLCalabashRuntime =>
        this.config = Some(runtime)
      case _ => throw XProcException.xiNotXMLCalabash()
    }
  }

  override def run(): Unit = {
    // nop
  }

  override def reset(): Unit = {
    bindings.clear()
  }

  override def abort(): Unit = {
    // nop
  }

  override def stop(): Unit = {
    // nop
  }

  override def toString: String = {
    val defStr = super.toString
    if (defStr.startsWith("com.xmlcalabash.steps")) {
      val objstr = ".*\\.([^\\.]+)@[0-9a-f]+$".r
      defStr match {
        case objstr(name) => name
        case _ => defStr

      }
    } else {
      defStr
    }
  }
}
