package com.xmlcalabash.steps.file

import com.xmlcalabash.namespace.NsP

class FileMoveStep(): FileCopyOrMove(NsP.fileMove) {
    override fun run() {
        super.run()
        copyOrMove()
    }
}