package com.xmlcalabash.steps.archives

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.io.IOUtils
import java.io.InputStream
import java.io.OutputStream

class XArchiveEntry private constructor(val stepConfig: XProcStepConfiguration, val name: String, val document: XProcDocument?, val entry: ArchiveEntry?, val archive: InputArchive? = null) {
    constructor(stepConfig: XProcStepConfiguration, name: String, doc: XProcDocument) : this(stepConfig, name, doc, null, null)
    constructor(stepConfig: XProcStepConfiguration, name: String, entry: ArchiveEntry, archive: InputArchive): this(stepConfig, name, null, entry, archive)

    var position = -1
    val properties = mutableMapOf<QName, String>()
    init {
        properties[Ns.name] = name
    }

    val method: String?
        get() = properties[Ns.method]

    val comment: String?
        get() = properties[Ns.comment]

    val inputStream: InputStream?
        get() {
            return archive?.inputStream(this)
        }

    fun write(stream: OutputStream) {
        if (document == null) {
            IOUtils.copy(inputStream!!, stream)
        } else {
            val serializer = XProcSerializer(stepConfig)
            serializer.write(document, stream, "archive")
        }
    }

    override fun toString(): String {
        if (archive != null) {
            return "${name} (${archive.archiveFormat})"
        }
        return name
    }
}