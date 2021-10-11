package com.xmlcalabash.config

import net.sf.saxon.s9api.QName

trait ErrorExplanation {
  def message(code: QName, variant: Int): String
  def message(code: QName, variant: Int, details: List[Any]): String
  def explanation(code: QName, variant: Int): String
  def explanation(code: QName, variant: Int, details: List[Any]): String
}
