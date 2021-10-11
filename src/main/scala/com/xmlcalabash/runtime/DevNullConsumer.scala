package com.xmlcalabash.runtime

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer

class DevNullConsumer extends DataConsumer {
  override def consume(port: String, message: Message): Unit = {
    // drop on the floor
  }
}
