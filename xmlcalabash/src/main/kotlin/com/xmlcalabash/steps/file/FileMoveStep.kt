package com.xmlcalabash.steps.file

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.NsP

class FileMoveStep(): FileCopyOrMove(NsP.fileMove) {
    override fun input(port: String, doc: XProcDocument) {
        // never called
    }

    override fun run() {
        super.run()
        copyOrMove()
    }
}