package com.xmlcalabash.runtime

trait XProcDataConsumer {
  def receive(port: String, item: Any, metadata: XProcMetadata): Unit
}
