package com.xmlcalabash.model.xxml

import com.jafpl.graph.Location
import com.xmlcalabash.util.{MinimalStaticContext, S9Api, XdmLocation}
import net.sf.saxon.s9api.{QName, XdmNode}

class XArtifactContext(val artifact: XArtifact, nodeName: QName, location: Option[Location]) extends XMLStaticContext(nodeName, location) {
  def this(artifact: XArtifact, node: XdmNode) = {
    this(artifact, node.getNodeName, XdmLocation.from(node))
    _inscopeNamespaces ++= S9Api.inScopeNamespaces(node)
  }

  def this(artifact: XArtifact, ctx: MinimalStaticContext, name: QName) = {
    this(artifact, name, ctx.location)
  }

  def constants: List[XNameBinding] = _inscopeConstants.toList

  def withConstant(const: XNameBinding): XArtifactContext = {
    val newContext = new XArtifactContext(artifact, nodeName, location)
    newContext._inscopeConstants ++= _inscopeConstants
    newContext._inscopeConstants += const
    newContext._inscopeNamespaces ++= _inscopeNamespaces
    newContext._baseURI = _baseURI
    newContext._location = _location
    newContext
  }

  def withConstants(constants: List[XNameBinding]): XArtifactContext = {
    val newContext = new XArtifactContext(artifact, nodeName, location)
    newContext._inscopeConstants ++= _inscopeConstants
    newContext._inscopeConstants ++= constants
    newContext._inscopeNamespaces ++= _inscopeNamespaces
    newContext._baseURI = _baseURI
    newContext._location = _location
    newContext
  }

  def withConstants(bcontext: XNameBindingContext): XArtifactContext = {
    if (bcontext.inScopeConstants.isEmpty) {
      return this
    }

    val newContext = new XArtifactContext(artifact, nodeName, location)
    newContext._inscopeConstants ++= _inscopeConstants
    newContext._inscopeConstants ++= bcontext.inScopeConstants.values
    newContext._inscopeNamespaces ++= _inscopeNamespaces
    newContext._baseURI = _baseURI
    newContext._location = _location
    newContext
  }
}
