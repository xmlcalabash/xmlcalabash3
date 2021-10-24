package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.{MediaType, XmlItemComparator}
import net.sf.saxon.s9api.QName

class XForUntil(config: XMLCalabash) extends XLoopingStep(config) {
  private val _max_iterations = new QName("max-iterations")
  private val _comparator = new QName("comparator")
  private val _return = new QName("return")
  private var maxIterations: Long = -1
  private var comparator: String = "deep-equal($a,$b)"
  private var returnSet: String = "last"

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()

    if (attributes.contains(_max_iterations)) {
      maxIterations = attr(_max_iterations).get.toInt
    }

    if (attributes.contains(_comparator)) {
      comparator = attr(_comparator).get
    }

    if (attributes.contains(_return)) {
      returnSet = attr(_return).get
      if (returnSet != "last" && returnSet != "all") {
        throw new RuntimeException("return must be last or all")
      }
    }
  }

  override protected[xxml] def validate(): Unit = {
    super.validate()

    var testOutput = Option.empty[XOutput]
    for (child <- children[XOutput]) {
      if (child.port == "test") {
        testOutput = Some(child)
      }
    }

    if (testOutput.isEmpty) {
      val output = new XOutput(this, Some("test"))
      output.primary = false
      output.sequence = false
      output.contentTypes = MediaType.MATCH_ANY

      val firstStep = children[XStep].head
      val step = children[XStep].last
      val pout = step.primaryOutput
      if (pout.isDefined) {
        val pipe = new XPipe(step.primaryOutput.get)
        output.addChild(pipe)
        if (Option(firstStep).isDefined) {
          insertBefore(output, firstStep)
        } else {
          addChild(output)
        }
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val compare = new XmlItemComparator(config, comparator, maxIterations, this)
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addUntil(compare, returnSet=="all", stepName, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
