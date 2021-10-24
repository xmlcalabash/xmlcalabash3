package com.xmlcalabash.runtime

import com.xmlcalabash.model.xxml.XDeclareStep

trait StepExecutable extends XmlStep {
  def declaration: XDeclareStep
}
