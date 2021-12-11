package com.xmlcalabash.model.xxml

import com.jafpl.graph.Location
import com.xmlcalabash.util.{MinimalStaticContext, S9Api, URIUtils, Urify, XdmLocation}
import net.sf.saxon.s9api.{QName, XdmNode}

import java.net.URI
import scala.collection.mutable

class XMLStaticContext(val nodeName: QName, location: Option[Location]) extends XStaticContext() {
  _location = location

  def this(nodeName: QName, location: Location) = {
    this(nodeName, Some(location))
  }

  def this(nodeName: QName, location: Location, nsmap: Map[String,String]) = {
    this(nodeName, Some(location))
    _inscopeNamespaces ++= nsmap
  }

  def this(node: XdmNode) = {
    this(node.getNodeName, XdmLocation.from(node))
    _inscopeNamespaces ++= S9Api.inScopeNamespaces(node)
  }

  def forNode(node: XdmNode): XMLStaticContext = {
    val newcontext = new XMLStaticContext(node)
    newcontext._config = _config
    newcontext._inscopeConstants ++= _inscopeConstants
    newcontext._inscopeNamespaces ++= _inscopeNamespaces
    newcontext._baseURI = Option(node.getBaseURI)
    newcontext
  }

  /*
  def withConstants(bcontext: XNameBindingContext): XMLStaticContext = {
    if (bcontext.inScopeConstants.isEmpty) {
      return this
    }

    val newContext = new XMLStaticContext(nodeName, location)
    newContext._inscopeConstants ++= inscopeConstants
    newContext._inscopeNamespaces ++= inscopeNamespaces
    newContext._baseURI = _baseURI
    newContext._location = _location

    for ((name,binding) <- bcontext.inScopeConstants) {
      newContext._inscopeConstants.put(name, binding)
    }

    newContext
  }

   */
}
