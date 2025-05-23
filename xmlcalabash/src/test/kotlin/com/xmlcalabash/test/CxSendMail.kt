package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.util.BufferingReceiver
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

class CxSendMail {
    companion object {
        private val url = URI("http://localhost:1080/api/messages/").toURL()
    }

    private fun clearMessages() {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        Assertions.assertEquals(200, conn.responseCode)
        conn.disconnect()
    }

    private fun getMessages(): JSONObject {
        Thread.sleep(500) // Give the server a chance to catch up...

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        Assertions.assertEquals(200, conn.responseCode)

        val input = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line = input.readLine()
        while (line != null) {
            sb.append(line)
            line = input.readLine()
        }

        input.close()
        conn.disconnect()

        val json = JSONObject(sb.toString())
        return json
    }

    @Test
    fun testPlainText() {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val declaration = parser.parse("src/test/resources/send-mail/pipe.xpl")

        val exec = declaration.getExecutable()
        val receiver = BufferingReceiver()
        exec.receiver = receiver

        clearMessages()

        try {
            exec.run()
        } catch (ex: Exception) {
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        Assertions.assertEquals("true", result.value.underlyingValue.stringValue)

        val json = getMessages()
        Assertions.assertEquals("OK", json.get("code"))
        val message = json.getJSONArray("data").getJSONObject(0)
        Assertions.assertEquals("text/plain", message.getString("type"))
        Assertions.assertEquals("Plain text email", message.getString("subject"))
    }

    @Test
    fun testHTML() {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val declaration = parser.parse("src/test/resources/send-mail/pipe-html.xpl")

        val exec = declaration.getExecutable()
        val receiver = BufferingReceiver()
        exec.receiver = receiver

        clearMessages()

        try {
            exec.run()
        } catch (ex: Exception) {
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        Assertions.assertEquals("true", result.value.underlyingValue.stringValue)

        val json = getMessages()
        Assertions.assertEquals("OK", json.get("code"))
        val message = json.getJSONArray("data").getJSONObject(0)
        Assertions.assertEquals("text/html", message.getString("type"))
        Assertions.assertEquals("HTML Email", message.getString("subject"))
    }

    @Test
    fun testMultipart() {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val declaration = parser.parse("src/test/resources/send-mail/pipe-mp.xpl")

        val exec = declaration.getExecutable()
        val receiver = BufferingReceiver()
        exec.receiver = receiver

        clearMessages()

        try {
            exec.run()
        } catch (ex: Exception) {
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        Assertions.assertEquals("true", result.value.underlyingValue.stringValue)

        val json = getMessages()
        Assertions.assertEquals("OK", json.get("code"))
        val message = json.getJSONArray("data").getJSONObject(0)
        Assertions.assertEquals("multipart/mixed", message.getString("type"))
        Assertions.assertEquals("Multi-part Email", message.getString("subject"))
    }
}