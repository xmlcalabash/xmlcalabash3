package com.xmlcalabash.model.xxml

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XPortBindingContext private(portMap: Map[String,XPort], internalPortMap: Map[String,XPort]) {
  def this() = {
    this(Map(), Map())
  }

  def withContainer(step: XContainer): XPortBindingContext = {
    val newPorts = step.publicPipeConnections
    val newInternalPorts = step.privatePipeConnections
    new XPortBindingContext(portMap ++ newPorts, internalPortMap ++ newInternalPorts)
  }

  def primaryPort(stepName: String): Option[XPort] = {
    val prefix = s"${stepName}/"
    for (port <- portMap.keySet) {
      if (port.startsWith(prefix)) {
        if (portMap(port).primary) {
          return portMap.get(port)
        }
      }
    }
    None
  }

  def port(pipe: XPipe): Option[XPort] = {
    val key = s"${pipe.step.getOrElse("???")}/${pipe.port.getOrElse("???")}"
    portMap.get(key)
  }

  def privatePort(pipe: XPipe): Option[XPort] = {
    val key = s"${pipe.step.getOrElse("???")}/${pipe.port.getOrElse("???")}"
    if (portMap.contains(key)) {
      portMap.get(key)
    } else {
      internalPortMap.get(key)
    }
  }

  def validate(pipe: XPipe): Boolean = {
    port(pipe).isDefined
  }
}
