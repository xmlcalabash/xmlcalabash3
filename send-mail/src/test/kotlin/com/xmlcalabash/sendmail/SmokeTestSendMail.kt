package com.xmlcalabash.sendmail

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.util.BufferingReceiver
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

// These tests require the Sendria server to be standing up.
// They also require single-threaded execution.

class SmokeTestSendMail {
    companion object {
        private val url = URL("http://localhost:1080/api/messages/")
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
        val declaration = parser.parse("src/test/resources/pipe.xpl")

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
        val declaration = parser.parse("src/test/resources/pipe-html.xpl")

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
        val declaration = parser.parse("src/test/resources/pipe-mp.xpl")

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