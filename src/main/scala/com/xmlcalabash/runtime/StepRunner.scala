package com.xmlcalabash.runtime
import com.jafpl.graph.Location
import com.jafpl.messages.{ExceptionMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortCardinality}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.StepSignature
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.xxml.{XDeclareStep, XInput, XOption, XOutput, XStaticContext}
import com.xmlcalabash.util.{MediaType, MinimalStaticContext, XProcVarValue}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class StepRunner(val decl: XDeclareStep) extends StepExecutable {
  private var runtime: XMLCalabashRuntime = _
  private var _location = Option.empty[Location]
  private val consumers = mutable.HashMap.empty[String, ConsumerMap]
  private val bindings = mutable.HashMap.empty[QName, XProcVarValue]
  private val inputs = mutable.HashMap.empty[String,ListBuffer[(Any, XProcMetadata)]]
  private var _usedPorts = Set.empty[String]

  private val cardMap = mutable.HashMap.empty[String,PortCardinality]
  private val typeMap = mutable.HashMap.empty[String,List[String]]
  for (input <- decl.children[XInput]) {
    if (input.sequence) {
      cardMap.put(input.port, new PortCardinality(0))
    } else {
      cardMap.put(input.port, new PortCardinality(1, 1))
    }

    val ctypes = ListBuffer.empty[String]
    for (ctype <- input.contentTypes) {
      ctypes += ctype.toString
    }
    typeMap.put(input.port, ctypes.toList)
  }
  private val iSpec = new XmlPortSpecification(cardMap.toMap, typeMap.toMap)

  cardMap.clear()
  typeMap.clear()
  for (output <- decl.children[XOutput]) {
    if (output.sequence) {
      cardMap.put(output.port, new PortCardinality(0))
    } else {
      cardMap.put(output.port, new PortCardinality(1, 1))
    }

    val ctypes = ListBuffer.empty[String]
    for (ctype <- output.contentTypes) {
      ctypes += ctype.toString
    }
    typeMap.put(output.port, ctypes.toList)
  }
  private val oSpec = new XmlPortSpecification(cardMap.toMap, typeMap.toMap)

  override def declaration: XDeclareStep = decl

  override def inputSpec: XmlPortSpecification = iSpec

  override def outputSpec: XmlPortSpecification = oSpec

  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    // It's too early to set in the runtime, save for later
    for (output <- decl.children[XOutput]) {
      consumers.put(output.port, new ConsumerMap(output.port, consumer))
    }
  }

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }

  override def receiveBinding(variable: NameValueBinding): Unit = {
    // It's too early to set in the runtime, save for later
    bindings.put(variable.name, new XProcVarValue(variable.value, new XStaticContext())) // FIXME: context?
  }

  // Input to the pipeline
  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    // It's too early to set in the runtime, save for later
    if (inputs.contains(port)) {
      val lb = inputs(port)
      lb += ((item, metadata))
    } else {
      val lb = new ListBuffer[(Any, XProcMetadata)]
      lb += ((item, metadata))
      inputs.put(port, lb)
    }
  }

  override def configure(config: XMLCalabash, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    // nop
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    // nop
  }

  def usedPorts(ports: Set[String]): Unit = {
    _usedPorts = ports
  }

  override def run(context: MinimalStaticContext): Unit = {
    //println("=======================================")
    //decl.dump()

    // If values have been passed in, they override defaults in the pipeline
    decl.patchOptions(bindings.toMap)

    runtime = decl.runtime()

    for ((port,lb) <- inputs) {
      for ((value,metadata) <- lb) {
        // An early attempt to deal with default bindings used a magic cx_use_default_input
        // element. I've removed that, but it was here.
        runtime.input(port, value, metadata)
      }
    }

    for ((name, value) <- bindings) {
      runtime.option(name, value.value, value.context)
    }

    for ((port, consumer) <- consumers) {
      runtime.output(port, consumer)
    }

    runtime.usedPorts(_usedPorts)
    runtime.run()
  }

  override def reset(): Unit = {
    if (runtime != null) {
      runtime.reset()
    }
    inputs.clear()
    bindings.clear()
  }

  override def abort(): Unit = {
    try {
      if (runtime != null) {
        runtime.stop()
      }
    } catch {
      case _: Exception => ()
    }
  }

  override def stop(): Unit = {
    if (runtime != null) {
      runtime.stop()
    }
  }

  class ConsumerMap(val result_port: String, val consumer: XProcDataConsumer) extends DataConsumer {
    override def consume(port: String, message: Message): Unit = {
      // The data consumer always receives input on its "source" port. We have to construct
      // this consumer so that it knows what output port to deliver to.

      // Get exceptions out of the way
      message match {
        case msg: ExceptionMessage =>
          msg.item match {
            case ex: XProcException =>
              if (ex.errors.isDefined) {
                consumer.receive(result_port, ex.errors.get, XProcMetadata.XML)
              } else {
                consumer.receive(result_port, msg.item, XProcMetadata.EXCEPTION)
              }
            case _ =>
              consumer.receive(result_port, msg.item, XProcMetadata.EXCEPTION)
          }
          return
        case _ => ()
      }

      message match {
        case msg: XdmValueItemMessage =>
          consumer.receive(result_port, msg.item, msg.metadata)
        case msg: AnyItemMessage =>
          consumer.receive(result_port, msg.shadow, msg.metadata)
        case _ =>
          throw XProcException.xiInvalidMessage(None, message)
      }
    }
  }
}
