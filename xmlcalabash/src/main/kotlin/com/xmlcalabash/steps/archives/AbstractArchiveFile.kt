package com.xmlcalabash.steps.archives

import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.s9api.QName
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists

abstract class AbstractArchiveFile(val stepConfig: XProcStepConfiguration) {
    abstract val archiveFormat: QName

    // Archives can be large. We probably don't need them in memory and the
    // XProcDocument class should have the ability to manage them on disk.
    // Various aspects of creating an archive file are easier if the file is
    // being written to disk, so let's just do that for now. (In particular,
    // the ZIP archiver is very fussy if the output isn't going to a file.)
    protected val tdir = if (System.getProperty("java.io.tmpdir") == null) {
        Paths.get(".")
    } else {
        Paths.get(System.getProperty("java.io.tmpdir"))
    }
    protected var temporary = false

    protected lateinit var archiveFile: Path

    open fun deleteIfTemporary() {
        if (temporary) {
            archiveFile.deleteIfExists()
        }
    }
}