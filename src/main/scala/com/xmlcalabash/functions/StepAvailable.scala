package com.xmlcalabash.functions

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xxml.{XArtifact, XDeclareStep}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Item, Sequence, StructuredQName}
import net.sf.saxon.s9api.QName
import net.sf.saxon.value.{BooleanValue, SequenceType}

class StepAvailable(runtime: XMLCalabash) extends FunctionImpl() {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "step-available")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_STRING)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ATOMIC

  override def makeCallExpression(): ExtensionFunctionCall = {
    new Call(this)
  }

  private class Call(funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    private var staticContext: StaticContext = _

    override def supplyStaticContext(context: StaticContext, locationId: Int, arguments: Array[Expression]): Unit = {
      super.supplyStaticContext(context, locationId, arguments)
      staticContext = context
    }

    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext.isEmpty) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val lexicalQName = arguments(0).head().getStringValue
      val propertyName = if (lexicalQName.trim.startsWith("Q{")) {
        StructuredQName.fromClarkName(lexicalQName)
      } else {
        StructuredQName.fromLexicalQName(lexicalQName, false, false, staticContext.getNamespaceResolver)
      }

      val stepType = new QName(propertyName.getPrefix, propertyName.getURI, propertyName.getLocalPart)
      if (runtime.externalSteps.contains(stepType)) {
        return new BooleanValue(true, BuiltInAtomicType.BOOLEAN)
      }

      val dc = exprEval.dynContext.get
      if (dc.artifact.isEmpty) {
        new BooleanValue(runtime.staticStepAvailable(stepType), BuiltInAtomicType.BOOLEAN)
      } else {
        var p: Option[XArtifact] = dc.artifact
        while (p.isDefined) {
          p.get match {
            case decl: XDeclareStep =>
              for (step <- decl.inScopeSteps) {
                if (step.stepType.isDefined && step.stepType.get == stepType) {
                  return new BooleanValue(!step.atomic || step.implementationClass.isDefined, BuiltInAtomicType.BOOLEAN)
                }
              }
            case _ => ()
          }
          p = p.get.parent
        }
        new BooleanValue(false, BuiltInAtomicType.BOOLEAN)
      }
    }
  }
}
