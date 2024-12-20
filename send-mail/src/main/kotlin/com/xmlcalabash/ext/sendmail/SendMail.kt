package com.xmlcalabash.ext.sendmail

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.*
import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class SendMail(): AbstractAtomicStep() {
    companion object {
        val NS_EMAIL = NamespaceUri.of("URN:ietf:params:email-xml:")
        val NS_RFC822 = NamespaceUri.of("URN:ietf:params:rfc822:")
        val HTML = NamespaceUri.of("http://www.w3.org/1999/xhtml")
        val em_Message = QName(NS_EMAIL, "em:Message")
        val em_Address = QName(NS_EMAIL, "em:Address")
        val em_name = QName(NS_EMAIL, "em:name")
        val em_adrs = QName(NS_EMAIL, "em:adrs")
        val em_content = QName(NS_EMAIL, "em:content")
        val _debug = QName("debug")
        val _host = QName("host")
        val _port = QName("port")
        val _username = QName("username")
        val _password = QName("password")
    }

    private lateinit var sendmail: Map<String,String>

    val sources = mutableListOf<XProcDocument> ()

    private var serialization = mapOf<QName,XdmValue>()
    private var auth = mapOf<QName,XdmValue>()
    val properties = Properties()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        sendmail = stepConfig.saxonConfig.xmlCalabash.xmlCalabashConfig.sendmail
    }

    override fun input(port: String, doc: XProcDocument) {
        sources.add(doc)
    }

    override fun run() {
        super.run()

        val parameters = qnameMapBinding(Ns.parameters)
        serialization = qnameMapBinding(Ns.serialization)
        auth = qnameMapBinding(Ns.serialization)

        val mainMessage = sources.removeFirst()
        val email = if (mainMessage.value is XdmNode) {
            S9Api.documentElement(mainMessage.value as XdmNode)
        } else {
            throw stepConfig.exception(XProcError.step(161, "p:send-mail source is not XML"))
        }

        if (email.nodeName != em_Message) {
            throw stepConfig.exception(XProcError.step(161, "p:send-mail source is not an em:Message"))
        }

        setProperty("mail.smtp.host", parameters[_host]?.underlyingValue?.stringValue ?: sendmail["host"])
        setProperty("mail.smtp.port", parameters[_port]?.underlyingValue?.stringValue ?: sendmail["port"])
        if (sendmail["username"] != null) {
            properties["mail.smtp.auth"] = "true"
        }
        if (auth[_username] != null) {
            properties["mail.smtp.auth"] = "true"
        }

        var mp: MimeMultipart? = null

        try {
            val auth = SMTPAuthenticator()
            val session = Session.getDefaultInstance(properties, auth)
            session.debug = parameters[_debug]?.underlyingValue?.stringValue == "true"

            val msg = MimeMessage(session)
            for (field in email.axisIterator(Axis.CHILD)) {
                if (field.nodeKind != XdmNodeKind.ELEMENT) {
                    continue
                }

                if (field.nodeName.namespaceUri == NS_RFC822) {
                    when (field.nodeName.localName) {
                        "to" -> {
                            val addrs = parseAddresses(field)
                            msg.setRecipients(Message.RecipientType.TO, addrs)
                        }
                        "from" -> {
                            val from = parseAddress(field)
                            msg.setFrom(from)
                        }
                        "sender" -> {
                            val sender = parseAddress(field)
                            msg.sender = sender
                        }
                        "subject" -> msg.setSubject(field.stringValue)
                        "cc" -> {
                            val addrs = parseAddresses(field)
                            msg.setRecipients(Message.RecipientType.CC, addrs)
                        }
                        "bcc" -> {
                            val addrs = parseAddresses(field)
                            msg.setRecipients(Message.RecipientType.BCC, addrs)
                        }
                        else -> throw stepConfig.exception(XProcError.step(161, "Unexpected RFC 822 element: ${field.nodeName.localName}"))
                    }
                } else if (field.nodeName == em_content) {
                    // What kind of content is this?
                    var text = false
                    var html = false

                    val builder = SaxonTreeBuilder(stepConfig)
                    builder.startDocument(email.baseURI)

                    for (child in field.axisIterator(Axis.CHILD)) {
                        builder.addSubtree(child)
                        if (!html && !text) {
                            if (child.nodeKind == XdmNodeKind.ELEMENT) {
                                html = child.nodeName.namespaceUri == HTML
                            } else if (child.nodeKind == XdmNodeKind.TEXT) {
                                text = child.stringValue.trim().isNotEmpty()
                            }
                        }
                    }

                    builder.endDocument()
                    val doc = XProcDocument.ofXml(builder.result, stepConfig)

                    var content = ""
                    var contentType = ""
                    if (html) {
                        val serializer = XProcSerializer(stepConfig)
                        val serprop = mutableMapOf<QName, XdmValue>(
                            QName("html-version") to XdmAtomicValue("5"),
                            QName("encoding") to XdmAtomicValue("UTF-8"),
                            QName("indent") to XdmAtomicValue("no"),
                            QName("omit-xml-declaration") to XdmAtomicValue("yes"),
                        )
                        serprop.putAll(serialization)
                        val stream = ByteArrayOutputStream()
                        serializer.write(doc, stream, MediaType.HTML, serprop)
                        content = stream.toString("UTF-8")
                        contentType = "text/html;charset=UTF-8"
                    } else {
                        content = field.stringValue.trim()
                        contentType = "text/plain;charset=UTF-8"
                    }

                    if (sources.isNotEmpty()) {
                        mp = MimeMultipart()
                        val bodyPart = MimeBodyPart()
                        val source = PartDataSource(content.toByteArray(), contentType, "irrelevant")
                        bodyPart.dataHandler = DataHandler(source)
                        bodyPart.disposition = Part.INLINE
                        mp.addBodyPart(bodyPart)
                    } else {
                        msg.setContent(content, contentType)
                    }
                } else {
                    throw stepConfig.exception(XProcError.step(161, "Unexpected element: ${field.nodeName.localName}"))
                }
            }

            while (sources.isNotEmpty()) {
                val doc = sources.removeFirst()
                val contentType = doc.contentType?.toString() ?: "application/octet-stream"
                var filename = doc.baseURI?.path ?: "unknown"
                val pos = filename.lastIndexOf("/")
                if (pos > 0) {
                    filename = filename.substring(pos+1)
                }

                val serializer = XProcSerializer(stepConfig)
                val stream = ByteArrayOutputStream()
                serializer.write(doc, stream)

                val bodyPart = MimeBodyPart()
                val source = PartDataSource(stream.toByteArray(), contentType, filename)
                bodyPart.dataHandler = DataHandler(source)
                bodyPart.fileName = filename
                bodyPart.disposition = Part.ATTACHMENT
                mp!!.addBodyPart(bodyPart)
            }

            if (mp != null) {
                msg.setContent(mp)
            }

            Transport.send(msg)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdStepFailed("p:send-mail failed"), ex)
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(stepConfig.baseUri)
        builder.addStartElement(NsC.result)
        builder.addText("true")
        builder.addEndElement()
        builder.endDocument()
        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    override fun reset() {
        super.reset()
        sources.clear()
    }

    override fun toString(): String = "p:send-mail"

    private fun setProperty(name: String, value: String?) {
        if (value != null) {
            properties[name] = value
        }
    }

    private fun parseAddress(field: XdmNode): InternetAddress {
        var email: InternetAddress? = null
        for (addr in field.axisIterator(Axis.CHILD)) {
            if (addr.nodeKind != XdmNodeKind.ELEMENT) {
                continue
            }

            if (addr.nodeName == em_Address) {
                if (email == null) {
                    email = parseEmail(addr)
                } else {
                    throw stepConfig.exception(XProcError.step(161, "Expected only a single email address in ${field.nodeName}"))
                }
            } else {
                throw stepConfig.exception(XProcError.step(161, "Only <em:Address> is supported in ${field.nodeName}"))
            }
        }

        return email!!
    }

    private fun parseAddresses(field: XdmNode): Array<InternetAddress> {
        val emails = mutableListOf<InternetAddress>()
        for (addr in field.axisIterator(Axis.CHILD)) {
            if (addr.nodeKind != XdmNodeKind.ELEMENT) {
                continue
            }

            if (addr.nodeName == em_Address) {
                emails.add(parseEmail(addr))
            } else {
                throw stepConfig.exception(XProcError.step(161, "Only <em:Address> is supported in ${field.nodeName}"))
            }
        }

        return emails.toTypedArray()
    }

    private fun parseEmail(address: XdmNode): InternetAddress {
        var email_name: String? = null
        var email_addr: String? = null

        for (addr in address.axisIterator(Axis.CHILD)) {
            if (addr.nodeKind != XdmNodeKind.ELEMENT) {
                continue
            }

            if (em_name.equals(addr.nodeName)) {
                email_name = addr.stringValue
            } else if (em_adrs.equals(addr.nodeName)) {
                email_addr = addr.stringValue
            } else {
                throw stepConfig.exception(XProcError.step(161, "Only <em:name> and <em:adrs> are supported in ${address.nodeName}"))
            }
        }

        if (email_addr == null) {
            throw stepConfig.exception(XProcError.step(161, "Email address specified without an <em:adrs>"))
        }

        if (email_addr.startsWith("mailto:")) {
            email_addr = email_addr.substring(7)
        }

        val email = if (email_name == null) {
            InternetAddress(email_addr)
        } else {
            InternetAddress(email_addr, email_name)
        }

        return email
    }

    inner class SMTPAuthenticator: Authenticator() {
        public override fun getPasswordAuthentication(): PasswordAuthentication {
            val username = auth[_username]?.underlyingValue?.stringValue ?: sendmail["username"] ?: ""
            val password = auth[_password]?.underlyingValue?.stringValue ?: sendmail["password"] ?: ""
            return PasswordAuthentication(username, password)
        }
    }

    inner class PartDataSource(private val data: ByteArray, private val contentType: String, private val name: String) : DataSource {
        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(data)
        }

        override fun getOutputStream(): OutputStream {
            throw RuntimeException("Attempt to call getOutputStream() on p:send-mail data source?")
        }

        override fun getContentType(): String {
            return contentType
        }

        override fun getName(): String {
            return name
        }
    }
}