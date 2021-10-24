package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.StepSignature
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class XDeclContainer(config: XMLCalabash) extends XContainer(config) {
  protected var _psvi_required = Option.empty[Boolean]
  protected var _xpath_version = Option.empty[Double]
  protected var _version = Option.empty[Double]
  protected var _skeletons: mutable.HashMap[QName, XSkeletonStepSignature] = mutable.HashMap.empty[QName, XSkeletonStepSignature]
  protected var _builtinLibraries: ListBuffer[XLibrary] = ListBuffer.empty[XLibrary]
  protected val _precedingStaticOptions: ListBuffer[XOption] = ListBuffer.empty[XOption]
  protected val _inScopeSteps: mutable.HashSet[XDeclareStep] = mutable.HashSet.empty[XDeclareStep]
  protected val _xoptions: mutable.HashMap[QName,XOption] = mutable.HashMap.empty[QName, XOption]


  def inScopeSteps: List[XDeclareStep] = _inScopeSteps.toList
  protected[xxml] def addInScopeSteps(steps: List[XDeclareStep]): Unit = {
    for (step <- steps) {
      if (step.visibility == "public") {
        _inScopeSteps += step
      } else {
        println("bang")
      }
    }
  }

  def builtinLibraries: List[XLibrary] = _builtinLibraries.toList
  protected[xxml] def builtinLibraries_=(libs: List[XLibrary]): Unit = {
    _builtinLibraries.clear()
    _builtinLibraries ++= libs
  }

  def findDeclaration(stepType: QName): Option[XDeclareStep] = {
    for (step <- inScopeSteps) {
      if (step.stepType.isDefined && step.stepType.get == stepType) {
        return Some(step)
      }
    }

    for (lib <- builtinLibraries) {
      val decl = lib.findDeclaration(stepType)
      if (decl.isDefined) {
        return decl
      }
    }

    None
  }

  protected[xxml] def xelaborate(): Unit = {
    val stepList = ListBuffer.empty[XDeclareStep]

    val seenOptions = ListBuffer.empty[XOption]
    val newChildren = ListBuffer.empty[XArtifact]
    for (child <- allChildren) {
      child match {
        case opt: XOption =>
          // Peek to see if this is a static option; it hasn't been validated
          // yet, so this will be a bit speculative...
          val static = opt.attributes.get(XProcConstants._static)
          if (static.isDefined && static.get == "true") {
            seenOptions += opt
          }
          newChildren += child
        case decl: XDeclareStep =>
          decl._precedingStaticOptions ++= _precedingStaticOptions
          decl._precedingStaticOptions ++= seenOptions.toList
          stepList += decl
        case _ =>
          newChildren += child
      }
    }

    allChildren = newChildren.toList

    val stepTypes = mutable.HashSet.empty[QName]
    for (step <- _inScopeSteps) {
      if (step.stepType.isDefined) {
        stepTypes += step.stepType.get
      }
    }

    this match {
      case step: XDeclareStep =>
        step.elaborateStepApi()
        step._inScopeSteps += step
        step._inScopeSteps ++= stepList
      case lib: XLibrary =>
        lib._inScopeSteps ++= stepList.toList
        lib._builtinLibraries = _builtinLibraries
        lib.elaborateLibraryApi()
      case _ =>
        throw XProcException.xiThisCantHappen("Step declaration container is neither declare-step nor library?")
    }

    for (step <- stepList) {
      step._inScopeSteps ++= _inScopeSteps
      step._builtinLibraries = _builtinLibraries
      step.elaborateStepApi()

      if (step.stepType.isDefined) {
        if (stepTypes.contains(step.stepType.get)) {
          throw XProcException.xsDupStepType(step.stepType.get, location)
        }
      }
    }

    validate()

    for (step <- stepList) {
      step.xelaborate()
    }
  }

  protected[xxml] def errors: List[Exception] = {
    this match {
      case decl: XDeclareStep =>
        errors(List(decl))
      case _ =>
        exceptions ++ errors(inScopeSteps)
    }
  }

  private def errors(steps: List[XDeclareStep]): List[Exception] = {
    val exlist = ListBuffer.empty[Exception]
    for (step <- steps) {
      exlist ++= step.exceptions
      //exlist ++= errors(step.inScopeSteps)
    }
    exlist.toList
  }

  protected[xxml] def skeletons: List[XSkeletonStepSignature] = _skeletons.values.toList

  protected[xxml] def findSkeleton(stepType: QName): Option[XSkeletonStepSignature] = {
    if (_skeletons.contains(stepType)) {
      _skeletons.get(stepType)
    } else {
      if (parent.isDefined) {
        declarationContainer.findSkeleton(stepType)
      } else {
        for (lib <- builtinLibraries) {
          if (lib.findSkeleton(stepType).isDefined) {
            return lib.findSkeleton(stepType)
          }
        }
        None
      }
    }
  }

  protected[xxml] def addSkeleton(skeleton: XSkeletonStepSignature): Unit = {
    _skeletons.put(skeleton.stepType, skeleton)
  }

  def inputPorts: Set[String] = {
    val ports = mutable.HashSet.empty[String]
    for (child <- children[XInput]) {
      ports += child.port
    }
    ports.toSet
  }

  def input(port: String): XInput = {
    for (child <- children[XInput]) {
      if (child.port == port) {
        return child
      }
    }
    throw XProcException.xiThisCantHappen(s"Attempt to get input '${port}' on decl that doesn't have one")
  }

  def outputPorts: Set[String] = {
    val ports = mutable.HashSet.empty[String]
    for (child <- children[XOutput]) {
      ports += child.port
    }
    ports.toSet
  }

  def output(port: String): XOutput = {
    for (child <- children[XOutput]) {
      if (child.port == port) {
        return child
      }
    }
    throw XProcException.xiThisCantHappen(s"Attempt to get output '${port}' on decl that doesn't have one")
  }

  def options: List[XNameBinding] = {
    children[XWithOption] ++ children[XOption]
  }

  def optionNames: Set[QName] = {
    val names = mutable.HashSet.empty[QName]
    children[XWithOption] foreach { names += _.name }
    children[XOption] foreach { names += _.name }
    names.toSet
  }

  def option(name: QName): Option[XOption] = _xoptions.get(name)

  protected[xxml] def extractSteps(): Unit = {
    val newChildren = ListBuffer.empty[XArtifact]
    for (child <- allChildren) {
      child match {
        case decl: XDeclareStep =>
          decl.extractSteps()
          _inScopeSteps += decl
        case step: XStep =>
          newChildren += step
        case _ =>
          newChildren += child
      }
    }

    allChildren = newChildren.toList
  }
}
