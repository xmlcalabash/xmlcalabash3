package com.xmlcalabash.testing

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.xxml.XOutput
import com.xmlcalabash.util.MediaType

import scala.collection.mutable.ListBuffer

class BufferingConsumer() extends DataConsumer {
  private val _items = ListBuffer.empty[XProcItemMessage]
  private val _mediaTypes = ListBuffer.empty[MediaType]

  def messages: List[XProcItemMessage] = _items.toList

  def mediaTypes: List[MediaType] = _mediaTypes.toList
  def mediaTypes_=(types: List[MediaType]): Unit = {
    _mediaTypes.clear()
    _mediaTypes ++= types
  }

  override def consume(port: String, message: Message): Unit = {
    message match {
      case msg: XProcItemMessage =>
        // Check that the message content type is allowed on the output port
        val metadata = msg.metadata;
        if (_mediaTypes.nonEmpty) {
          if (!metadata.contentType.allowed(_mediaTypes.toList)) {
            throw XProcException.xdBadOutputMediaType(metadata.contentType, _mediaTypes.toList, None)
          }
        }
        _items += msg
      case _ =>
        throw XProcException.xiInvalidMessage(None, message)
    }
  }
}
