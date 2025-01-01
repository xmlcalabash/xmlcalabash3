package com.xmlcalabash.steps.archives

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.File

class JarOutputArchive(stepConfig: XProcStepConfiguration): ZipOutputArchive(stepConfig) {
    override val archiveFormat = Ns.jar

    override fun create(file: File?) {
        if (file == null) {
            archiveFile = createArchive(".jar")
        } else {
            archiveFile = file.toPath()
        }

        zipStream = ZipArchiveOutputStream(archiveFile)
    }
}