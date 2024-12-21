package com.xmlcalabash.steps.file

import com.xmlcalabash.namespace.NsP

class FileCopyStep(): FileCopyOrMove(NsP.fileCopy) {
    override fun run() {
        super.run()
        copyOrMove()
    }
}