package com.xmlcalabash.config

import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class Signatures {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _steps = mutable.HashMap.empty[QName, StepSignature]
  private val _functions = mutable.HashMap.empty[QName, List[String]]

  def addStep(step: StepSignature): Unit = {
    if (step.stepType.isDefined) {
      if (_steps.contains(step.stepType.get)) {
        logger.warn(s"Duplicate definition of ${step.stepType}")
      }
      _steps.put(step.stepType.get, step)
    }
  }

  def stepTypes: Set[QName] = _steps.keySet.toSet
  def step(stepType: QName): StepSignature = _steps(stepType)

  def addFunction(name: QName, className: String): Unit = {
    val list = _functions.getOrElse(name, List.empty[String])
    _functions.put(name, list ++ List(className))
  }

  def functions: Set[QName] = _functions.keySet.toSet
  def function(name: QName): List[String] = _functions(name)
}
