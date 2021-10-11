package com.xmlcalabash.runtime

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.xml.DeclareOutput

import scala.collection.mutable.ListBuffer

class BufferingConsumer(output: DeclareOutput) extends DataConsumer {
  private val _items = ListBuffer.empty[XProcItemMessage]

  def messages: List[XProcItemMessage] = _items.toList

  override def consume(port: String, message: Message): Unit = {
    message match {
      case msg: XProcItemMessage =>
        // Check that the message content type is allowed on the output port
        val mtypes = output.contentTypes;
        val metadata = msg.metadata;
        if (mtypes.nonEmpty) {
          if (!metadata.contentType.allowed(mtypes)) {
            throw XProcException.xdBadOutputMediaType(metadata.contentType, mtypes, output.location)
          }
        }
        _items += msg
      case _ =>
        throw XProcException.xiInvalidMessage(None, message)
    }
  }
}
