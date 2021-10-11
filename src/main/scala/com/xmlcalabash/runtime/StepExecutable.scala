package com.xmlcalabash.runtime

import com.xmlcalabash.config.StepSignature

trait StepExecutable extends XmlStep {
  def signature: StepSignature
}
