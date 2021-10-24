package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.XmlItemTester
import net.sf.saxon.s9api.QName

class XForWhile(config: XMLCalabash) extends XLoopingStep(config) {
  private val _max_iterations = new QName("max-iterations")
  private val _return = new QName("return")
  private var maxIterations: Long = -1
  private var test: String = ""
  private var returnSet: String = "last"

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()

    if (attributes.contains(_max_iterations)) {
      maxIterations = attr(_max_iterations).get.toInt
    }

    if (attributes.contains(XProcConstants._test)) {
      test = attr(XProcConstants._test).get
    } else {
      throw new RuntimeException("test is required")
    }

    if (attributes.contains(_return)) {
      returnSet = attr(_return).get
      if (returnSet != "last" && returnSet != "all") {
        throw new RuntimeException("return must be last or all")
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val tester = new XmlItemTester(runtime, test, maxIterations, this)
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addWhile(tester, returnSet == "all", stepName, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
