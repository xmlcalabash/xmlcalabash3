package com.xmlcalabash.steps.archives

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.io.IOUtils
import java.io.InputStream
import java.io.OutputStream

class XArchiveEntry private constructor(val stepConfig: StepConfiguration, val name: String, val document: XProcDocument?, val entry: ArchiveEntry?, val archive: InputArchive? = null) {
    constructor(stepConfig: StepConfiguration, name: String, doc: XProcDocument) : this(stepConfig, name, doc, null, null)
    constructor(stepConfig: StepConfiguration, name: String, entry: ArchiveEntry, archive: InputArchive): this(stepConfig, name, null, entry, archive)

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
            DocumentWriter(document, stream).write()
        }
    }

    override fun toString(): String {
        if (archive != null) {
            return "${name} (${archive.archiveFormat})"
        }
        return name
    }
}