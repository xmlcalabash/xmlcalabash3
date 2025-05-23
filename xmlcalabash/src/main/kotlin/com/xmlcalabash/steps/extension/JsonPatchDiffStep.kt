package com.xmlcalabash.steps.extension

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.diff.JsonDiff
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class JsonPatchDiffStep(): AbstractJsonPatch() {
    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        val target = queues["target"]!!.first()

        val mapper = ObjectMapper()

        // Kindof hacky; we'll put the JSON back into a string so we can parse it again :-(
        val sourceNode = mapper.readTree(jsonString(source))
        val targetNode = mapper.readTree(jsonString(target))
        val diff = JsonDiff.asJson(sourceNode, targetNode)

        val diffJson = mapper.writeValueAsString(diff)
        val loader = DocumentLoader(stepConfig, null)
        val stream = ByteArrayInputStream(diffJson.toByteArray(StandardCharsets.UTF_8))
        val value = loader.load(stream, MediaType.JSON)
        receiver.output("result", value)
    }

    override fun toString(): String = "cx:json-diff"
}