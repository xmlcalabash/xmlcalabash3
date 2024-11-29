package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.PipelineBuilder
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.*

class XplDocument(val builder: PipelineBuilder, val xml: XdmNode) {
    val exportedStaticOptions = mutableMapOf<OptionNode, XdmValue>()
    val exportedStepTypes = mutableMapOf<QName, StepImplementation>()
    val document = AnyNode(null, builder.stepConfig, xml)
    private val nodeMap = mutableMapOf<XdmNode, ElementNode>()
    private var _rootNode: RootNode
    val rootNode: RootNode
        get() = _rootNode

    init {
        if (xml.nodeKind != XdmNodeKind.DOCUMENT) {
            throw XProcError.xiImpossible("Document isn't a document node").exception()
        }
        try {
            constructTree(document)
        } catch (ex: XProcException) {
            throw ex.error.asStatic().exception()
        }
        val node = document.children.filterIsInstance<ElementNode>().first()
        when (node) {
            is DeclareStepNode -> _rootNode = node
            is LibraryNode -> _rootNode = node
            else -> throw XProcError.xsNotAPipeline(node.node.nodeName).exception()
        }
    }

    private fun constructTree(tree: AnyNode) {
        var inline = false
        var tnode: AnyNode? = tree
        while (!inline && tnode != null) {
            inline = tnode.node.nodeName == NsP.inline
            tnode = tnode.parent
        }

        for (node in tree.node.axisIterator(Axis.CHILD)) {
            val child = if (node.nodeKind == XdmNodeKind.ELEMENT) {
                if (inline) {
                    ElementNode(tree, node)
                } else {
                    when (node.nodeName) {
                        NsP.declareStep -> DeclareStepNode(tree, node)
                        NsP.library -> LibraryNode(tree, node)
                        NsP.import -> {
                            if (node.getAttributeValue(Ns.href) != null) {
                                ImportNode(tree, node)
                            } else {
                                ElementNode(tree, node)
                            }
                        }
                        NsP.importFunctions -> {
                            if (node.getAttributeValue(Ns.href) != null) {
                                ImportFunctionsNode(tree, node)
                            } else {
                                ElementNode(tree, node)
                            }
                        }
                        NsP.option -> {
                            if (node.getAttributeValue(Ns.name) != null
                                && node.getAttributeValue(Ns.select) != null
                                && node.getAttributeValue(Ns.static) != null
                                && parseBoolean(node.getAttributeValue(Ns.static))) {
                                try {
                                    val name = tree.stepConfig.parseQName(node.getAttributeValue(Ns.name))
                                    OptionNode(tree, node, name, node.getAttributeValue(Ns.select)!!)
                                } catch (ex: Exception) {
                                    ElementNode(tree, node)
                                }
                            } else {
                                ElementNode(tree, node)
                            }
                        }
                        else -> ElementNode(tree, node)
                    }
                }
            } else {
                if (node.nodeKind == XdmNodeKind.TEXT) {
                    TextNode(tree, node)
                } else {
                    AnyNode(tree, node)
                }
            }

            tree.children.add(child)
            if (child is ElementNode) {
                nodeMap[node] = child
            }
            constructTree(child)
        }
    }

    private fun parseBoolean(bool: String): Boolean {
        return bool == "true" || bool == "1"
    }

    internal fun resolve(manager: XplDocumentManager, context: UseWhenContext) {
        rootNode.resolve(manager, context)
        if (context.useWhen.isNotEmpty()) {
            val sb = StringBuilder()
            var first = true
            for (element in context.useWhen) {
                if (!first) {
                    sb.append("; ")
                }
                val cond = element.conditional!!
                if (cond.contains("\"") && cond.contains("'")) {
                    sb.append('"').append(cond.replace("\"", "&quot;")).append('"')
                } else if (cond.contains("\"")) {
                    sb.append("'").append(cond).append("'")
                } else {
                    sb.append("\"").append(cond).append("\"")
                }

                first = false
            }
            throw XProcError.xsUseWhenDeadlock(sb.toString()).exception()
        }

        when (rootNode) {
            is DeclareStepNode -> {
                val decl = rootNode as DeclareStepNode
                decl.visible = true // visibility is ignored if the declare step is at the top level
                if (decl.type != null) {
                    val impl = if (decl.useWhen == null) {
                        StepImplementation(false, false)
                    } else {
                        val available = decl.useWhen == true
                                && (!decl.isAtomic || decl.stepConfig.rteContext.atomicStepAvailable(decl.type!!))
                        StepImplementation(true, available)
                    }
                    exportedStepTypes[decl.type!!] = impl
                }
            }
            is LibraryNode -> {
                for ((option, value) in context.staticOptions) {
                    if (option.visible && value != null) {
                        exportedStaticOptions[option] = value
                    }
                }

                val decl = rootNode as LibraryNode
                for (child in decl.children.filterIsInstance<DeclareStepNode>()) {
                    if (child.visible && child.type != null) {
                        val impl = if (child.useWhen == null) {
                            StepImplementation(false, false)
                        } else {
                            val available = child.useWhen == true
                                    && (!child.isAtomic || decl.stepConfig.rteContext.atomicStepAvailable(child.type!!))
                            StepImplementation(true, available)
                        }
                        exportedStepTypes[child.type!!] = impl
                    }
                }
                for (child in decl.children.filterIsInstance<ImportNode>()) {
                    if (child.useWhen == true) {
                        val doc = manager.load(child.href)
                        exportedStepTypes.putAll(doc.exportedStepTypes)
                    }
                }
            }
            else -> throw XProcError.xiImpossible("Unexpected node type in XplDocument").exception()
        }
    }

    override fun toString(): String {
        return xml.baseURI?.toString() ?: "<no base uri>"
    }
}