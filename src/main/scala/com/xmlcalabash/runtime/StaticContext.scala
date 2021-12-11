package com.xmlcalabash.runtime

import java.net.URI
import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.model.xxml.{XArtifact, XNameBinding, XOption}
import com.xmlcalabash.util.{MinimalStaticContext, S9Api, URIUtils}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class StaticContext(val config: XMLCalabash) extends MinimalStaticContext() {
  protected var _baseURI: Option[URI] = None
  protected var _inScopeNS = Map.empty[String,String]
  protected var _location: Option[Location] = None
  protected var _constants = Map.empty[String,Message]

  def this(runtime: XMLCalabashRuntime) = {
    this(runtime.config)
  }

  def this(context: StaticContext) = {
    this(context.config)
    _baseURI = context._baseURI
    _inScopeNS = context._inScopeNS
    _location = context._location
    _constants = context._constants
  }

  def this(config: XMLCalabash, node: XdmNode) = {
    this(config)
    _baseURI = Option(node.getBaseURI)
    _inScopeNS = S9Api.inScopeNamespaces(node)
    _location = Some(new XProcLocation(node))
  }

  def baseURI: Option[URI] = _baseURI
  protected[xmlcalabash] def baseURI_=(uri: URI): Unit = {
    _baseURI = Some(uri)
  }

  def nsBindings: Map[String,String] = _inScopeNS
  protected[xmlcalabash] def nsBindings_=(bindings: Map[String,String]): Unit = {
    _inScopeNS = bindings
  }

  def location: Option[Location] = _location
  protected[xmlcalabash] def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  override def inscopeNamespaces: Map[String, String] = _inScopeNS

  override def inscopeConstants: Map[QName, XOption] = Map()

  def constants: Map[String,Message] = _constants

  def withConstants(constants: Map[String,Message]): StaticContext = {
    val context = new StaticContext(this)
    context._constants = constants
    context
  }

  def withConstants(bindings: List[XNameBinding]): StaticContext = {
    val constants = mutable.HashMap.empty[String, Message]
    for (bind <- bindings) {
      constants.put(bind.name.getClarkName, bind.constantValue.get)
    }
    withConstants(constants.toMap)
  }
}
