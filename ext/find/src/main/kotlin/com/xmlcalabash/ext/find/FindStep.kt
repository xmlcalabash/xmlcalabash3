package com.xmlcalabash.ext.find

import com.jayway.jsonpath.Option
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.ext.jsonpath.JsonPath
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.file.DirectoryDir
import com.xmlcalabash.steps.file.DirectoryEntry
import com.xmlcalabash.steps.file.DirectoryFile
import com.xmlcalabash.steps.file.DirectoryListStep
import net.sf.saxon.s9api.*
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class FindStep(): DirectoryListStep() {
    companion object {
        val _xpath = QName("xpath")
        val _jsonPath = QName("jsonpath")
        val _grep = QName("grep")
        val _line = QName("line")
    }

    val xpath = mutableListOf<String>()
    val jsonPath = mutableListOf<String>()
    val grep = mutableMapOf<String, XPathSelector>()

    override fun run() {
        if (options.containsKey(_xpath)) {
            val include = valueBinding(_xpath)
            for (item in include.value.iterator()) {
                xpath.add(item.stringValue)
            }
        }

        if (options.containsKey(_jsonPath)) {
            val include = valueBinding(_jsonPath)
            for (item in include.value.iterator()) {
                jsonPath.add(item.stringValue)
            }
        }

        if (options.containsKey(_grep)) {
            val compiler = stepConfig.newXPathCompiler()
            compiler.declareVariable(_line)
            val include = valueBinding(_grep)
            for (item in include.value.iterator()) {
                val exec = compiler.compile("matches(\$line, \"${item.stringValue.replace("\"", "\"\"")}\")")
                grep[item.stringValue] = exec.load()
            }
        }

        var queryCount = 0
        if (xpath.isNotEmpty()) {
            queryCount++
        }
        if (jsonPath.isNotEmpty()) {
            queryCount++
        }
        if (grep.isNotEmpty()) {
            queryCount++
        }

        if (queryCount > 1) {
            throw stepConfig.exception(XProcError.xcxAtMostOneQuery())
        }

        super.run()
    }

    override fun matchingFiles(dir: File, depth: Int): DirectoryEntry {
        val hierarchy = super.matchingFiles(dir, depth)
        filter(hierarchy as DirectoryDir, 1)
        return hierarchy
    }

    private fun filter(dir: DirectoryDir, depth: Int) {
        val filtered = mutableListOf<DirectoryEntry>()
        for (entry in dir.entries) {
            if (entry is DirectoryDir) {
                filter(entry, depth+1)
                if (entry.entries.isNotEmpty()) {
                    filtered.add(entry)
                }
            } else {
                if (matches(entry as DirectoryFile)) {
                    filtered.add(entry)
                }
            }
        }
        dir.entries.clear()
        dir.entries.addAll(filtered)
    }

    private fun matches(entry: DirectoryFile): Boolean {
        if (xpath.isNotEmpty()) {
            return matchesXPath(entry)
        }
        if (jsonPath.isNotEmpty()) {
            return matchesJsonPath(entry)
        }
        if (grep.isNotEmpty()) {
            return matchesGrep(entry)
        }
        return true
    }

    private fun matchesXPath(entry: DirectoryFile): Boolean {
        val properties = DocumentProperties()
        properties.set(Ns.contentType, "application/xml")
        val doc = try {
            stepConfig.documentManager.load(entry.file.toURI(), stepConfig, properties)
        } catch (_: Exception) {
            return false
        }

        val compiler = stepConfig.newXPathCompiler()
        for (expr in xpath) {
            try {
                val exec = compiler.compile(expr)
                val selector = exec.load()
                selector.contextItem = doc.value as XdmNode
                if (selector.effectiveBooleanValue()) {
                    stepConfig.debug { "${expr} in ${entry.file.absolutePath} EBV is true" }
                    return true
                } else {
                    stepConfig.debug { "${expr} in ${entry.file.absolutePath} EBV is false" }
                }
            } catch (ex: SaxonApiException) {
                throw stepConfig.exception(XProcError.xcxBadXPathInFind(expr), ex)
            }
        }

        return false
    }

    private fun matchesJsonPath(entry: DirectoryFile): Boolean {
        val properties = DocumentProperties()
        properties.set(Ns.contentType, "application/json")
        val doc = try {
            stepConfig.documentManager.load(entry.file.toURI(), stepConfig, properties)
        } catch (_: Exception) {
            stepConfig.debug { "${entry.file.absolutePath} is not valid JSON" }
            return false
        }

        val options = setOf(
            Option.ALWAYS_RETURN_LIST, Option.AS_PATH_LIST,
            Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS)

        for (expr in jsonPath) {
            val doc = JsonPath.evaluate(stepConfig, doc, expr, options)
            val value = doc.value
            if (value is XdmArray) {
                stepConfig.debug { "${expr} in ${entry.file.absolutePath} returned ${value}" }
                for (member in value.asList()) {
                    if (member != XdmEmptySequence.getInstance()) {
                        return true
                    }
                }
            } else {
                stepConfig.debug { "${expr} in ${entry.file.absoluteFile} returned non-array ${value}" }
            }
        }

        return false
    }

    private fun matchesGrep(entry: DirectoryFile): Boolean {
        FileInputStream(entry.file).use { stream ->
            InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                BufferedReader(reader).use { buf ->
                    var line = buf.readLine()
                    while (line != null) {
                        for ((regex, selector) in grep) {
                            selector.setVariable(_line, XdmAtomicValue(line))
                            try {
                                if (selector.effectiveBooleanValue()) {
                                    stepConfig.debug { "Grep in ${entry.file.absolutePath} matched ${line}" }
                                    return true
                                }
                            } catch (ex: SaxonApiException) {
                                throw stepConfig.exception(XProcError.xcxBadRegexInFind(regex), ex)
                            }
                        }
                        line = buf.readLine()
                    }
                    stepConfig.debug { "Grep did not match in ${entry.file.absolutePath}" }
                }
            }
        }
        return false
    }

    override fun toString(): String = "cx:find"
}