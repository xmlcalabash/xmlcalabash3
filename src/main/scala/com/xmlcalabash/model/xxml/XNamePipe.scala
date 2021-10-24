package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.XMLCalabashRuntime

import scala.collection.mutable

class XNamePipe(config: XMLCalabash, val binding: XNameBinding) extends XArtifact(config) {
  staticContext = binding.staticContext

  def this(binding: XNameBinding) = {
    this(binding.config, binding)
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("name", Some(binding.name))
    attr.put("ref", Some(binding.tumble_id))
    dumpTree(sb, "p:name-pipe", attr.toMap)
  }
}
