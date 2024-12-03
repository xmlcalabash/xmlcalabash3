import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths
import javax.xml.transform.dom.DOMSource

class Html5Parser {
  companion object {
    fun parse(sourcefn: File, outputfn: File) {
      val url = sourcefn.toURI().toURL()
      val conn = url.openConnection()
      val htmlBuilder = HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
      val html = htmlBuilder.parse(conn.getInputStream())

      val processor = Processor(false)
      val builder = processor.newDocumentBuilder()
      val doc = builder.build(DOMSource(html))

      val output = PrintStream(outputfn)

      val serializer = processor.newSerializer(output)
      serializer.setOutputProperty(QName("", "method"), "xhtml");
      serializer.setOutputProperty(QName("", "omit-xml-declaration"), "yes");
      serializer.setOutputProperty(QName("", "indent"), "no");
      serializer.serializeXdmValue(doc);
    }
  }
}
