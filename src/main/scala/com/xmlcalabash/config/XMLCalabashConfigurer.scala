package com.xmlcalabash.config

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.util.PipelineParameter

trait XMLCalabashConfigurer {
  def configure(input: List[PipelineParameter]): List[PipelineParameter]
  def update(config: XMLCalabash): Unit
}
