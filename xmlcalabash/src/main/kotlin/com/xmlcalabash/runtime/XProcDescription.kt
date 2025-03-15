package com.xmlcalabash.runtime

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsDescription
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode

class XProcDescription(val stepConfig: XProcStepConfiguration) {
    private val _pipelines = mutableListOf<XdmNode>()
    private val _graphs = mutableListOf<XdmNode>()
    private val names = mutableListOf<String>()
    private val uniqueNames = mutableSetOf<String>()
    private var xml: XdmNode? = null

    val pipelines: List<XdmNode>
        get() = _pipelines

    val graphs: List<XdmNode>
        get() = _graphs

    fun pipelineName(pipeline: XdmNode): String {
        for ((index, node) in _pipelines.withIndex()) {
            if (pipeline == node) {
                return names[index]
            }
        }
        throw IllegalStateException("Pipeline name is undefined")
    }

    fun graphName(graph: XdmNode): String {
        for ((index, node) in _graphs.withIndex()) {
            if (graph == node) {
                return names[index]
            }
        }
        throw IllegalStateException("Graph name is undefined")
    }

    fun addPipeline(node: XdmNode) {
        val root = S9Api.documentElement(node)
        val rawname= root.getAttributeValue(Ns.name) ?: "pipeline"
        val name = if (rawname.startsWith("!")) {
            val typeName = root.getAttributeValue(Ns.type) ?: "pipeline"
            unique(typeName.replace(":", "_"))
        } else {
            unique(rawname)
        }

        _pipelines.add(node)
        names.add(name)
    }

    fun addGraph(node: XdmNode) {
        _graphs.add(node)
    }

    fun replacePipelines(newPipelines: List<XdmNode>) {
        _pipelines.clear()
        _pipelines.addAll(newPipelines)
    }

    fun replaceGraphs(newGraphs: List<XdmNode>) {
        _graphs.clear()
        _graphs.addAll(newGraphs)
    }

    private fun unique(name: String): String {
        var unique = name
        var count = 1
        while (uniqueNames.contains(unique)) {
            count++
            unique = "${name}_${count}"
        }
        uniqueNames.add(unique)
        return unique
    }

    fun xml(): XdmNode {
        if (xml == null) {
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(null)
            builder.addStartElement(NsDescription.g("description"))

            for (pipeline in pipelines) {
                builder.addSubtree(pipeline)
            }
            for (graph in graphs) {
                builder.addSubtree(graph)
            }

            builder.addEndElement()
            builder.endDocument()
            xml = builder.result
        }
        return xml!!
    }
}