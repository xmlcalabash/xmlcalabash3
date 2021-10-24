package com.xmlcalabash.model.xxml

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class XNameBindingContext private(val inScopeStatics: Set[QName],
                                  val inScopeConstants: Map[QName, XNameBinding],
                                  val inScopeDynamics: Map[QName, XNameBinding]) {
  def this() = {
    this(Set(), Map(), Map())
  }

  def withBinding(binding: XNameBinding): XNameBindingContext = {
    if (inScopeStatics.contains(binding.name)) {
      throw XProcException.xsShadowsStatic(binding.name, binding.location)
    }

    val statics = mutable.Set.empty[QName] ++ inScopeStatics
    if (binding.static) {
      statics += binding.name
    }

    val constants = mutable.HashMap.empty[QName, XNameBinding] ++= inScopeConstants
    val dynamics = mutable.HashMap.empty[QName, XNameBinding] ++= inScopeDynamics

    if (binding.constant) {
      constants.put(binding.name, binding)
    } else {
      dynamics.put(binding.name, binding)
    }

    new XNameBindingContext(statics.toSet, constants.toMap, dynamics.toMap)
  }

  def onlyStatics: XNameBindingContext = {
    val constants = mutable.HashMap.empty[QName, XNameBinding]
    for ((name, value) <- inScopeConstants) {
      if (inScopeStatics.contains(name)) {
        constants.put(name, value)
      }
    }
    new XNameBindingContext(inScopeStatics, constants.toMap, Map())
  }
}
