package com.xmlcalabash.runtime

import net.sf.saxon.s9api.Processor

trait XMLCalabashProcessor {
  def processor: Processor
}
