package com.xmlcalabash.util

import com.vladsch.flexmark.util.data.MutableDataSet

class DefaultMarkdownConfigurer: MarkdownConfigurer {
    override fun configure(options: MutableDataSet, param: String) {
        // This doesn't do anything, it's just for testing
        // println("Called default configurer: ${param}")
    }
}