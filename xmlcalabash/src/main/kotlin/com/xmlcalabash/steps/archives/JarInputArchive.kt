package com.xmlcalabash.steps.archives

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import java.io.InputStream

class JarInputArchive(stepConfig: XProcStepConfiguration, doc: XProcBinaryDocument): ZipInputArchive(stepConfig, doc) {
    override val archiveFormat = Ns.jar
    override val baseUri = doc.baseURI
    override fun open() {
        openZip(".jar")
    }
}