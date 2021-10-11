package com.xmlcalabash.config

import java.io.InputStream

trait DocumentManager {
  def parse(request: DocumentRequest): DocumentResponse
  def parse(request: DocumentRequest, stream: InputStream): DocumentResponse
}
