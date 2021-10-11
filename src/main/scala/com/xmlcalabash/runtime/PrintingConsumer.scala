package com.xmlcalabash.runtime

import java.io.{ByteArrayOutputStream, File, FileOutputStream, PrintStream}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XProcItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.UniqueId
import com.xmlcalabash.model.xml.DeclareOutput
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{QName, Serializer}

class PrintingConsumer private(config: XMLCalabashRuntime, output: DeclareOutput, outputs: Option[List[String]]) extends DataConsumer {
  private val _id = UniqueId.nextId.toString
  private var index = 0

  def this(config: XMLCalabashRuntime, output: DeclareOutput) = {
    this(config, output, None)
  }

  def this(config: XMLCalabashRuntime, output: DeclareOutput, outputs: List[String]) = {
    this(config, output, Some(outputs))
  }

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

        val ctype = msg.metadata.contentType

        val pos = if (outputs.isEmpty || (index >= outputs.get.length)) {
          System.out
        } else {
          val file = new File(outputs.get(index))
          index += 1

          val fos = new FileOutputStream(file)
          new PrintStream(fos)
        }

        message match {
          case msg: AnyItemMessage =>
            val instream = msg.shadow.stream
            val outstream = new ByteArrayOutputStream()
            val buf = Array.fill[Byte](4096)(0)
            var len = instream.read(buf, 0, buf.length)
            while (len >= 0) {
              outstream.write(buf, 0, len)
              len = instream.read(buf, 0, buf.length)
            }
            pos.write(outstream.toByteArray)
          case msg: XdmValueItemMessage =>
            val stream = new ByteArrayOutputStream()
            val serializer = config.processor.newSerializer(stream)

            S9Api.configureSerializer(serializer, config.defaultSerializationOptions(ctype.toString))
            S9Api.configureSerializer(serializer, output.serialization)

            if (!ctype.xmlContentType) {
              serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
            }

            S9Api.serialize(config.config, msg.item, serializer)
            pos.print(stream.toString("UTF-8"))
          case _ =>
            throw new RuntimeException(s"Don't know how to print ${msg.item}")
        }
    }
  }
}
