package com.xmlcalabash.ext.jsonpatch

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.JsonPatch
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsXs.QName
import net.sf.saxon.s9api.QName
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class JsonPatchStep(): AbstractJsonPatch() {
    companion object {
        private val _merge = QName("merge")
    }

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        val patch = queues["patch"]!!.first()
        val merge = booleanBinding(_merge)!!

        val mapper = ObjectMapper()

        // Kindof hacky; we'll put the JSON back into a string so we can parse it again :-(
        val sourceNode = mapper.readTree(jsonString(source))
        val patchNode = mapper.readTree(jsonString(patch))

        val patched = if (merge) {
            val jsonPatch = JsonMergePatch.fromJson(patchNode)
            jsonPatch.apply(sourceNode)
        } else {
            val jsonPatch = JsonPatch.fromJson(patchNode)
            jsonPatch.apply(sourceNode)
        }

        val patchedJson = mapper.writeValueAsString(patched)
        val loader = DocumentLoader(stepConfig, null)
        val stream = ByteArrayInputStream(patchedJson.toByteArray(StandardCharsets.UTF_8))
        val value = loader.load(stream, MediaType.JSON)
        receiver.output("result", value)
    }

    override fun toString(): String = "cx:json-patch"
}