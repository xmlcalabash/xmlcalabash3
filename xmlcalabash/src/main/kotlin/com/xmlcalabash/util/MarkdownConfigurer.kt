package com.xmlcalabash.util

import com.vladsch.flexmark.util.data.MutableDataSet

interface MarkdownConfigurer {
    fun configure(options: MutableDataSet, param: String)
}