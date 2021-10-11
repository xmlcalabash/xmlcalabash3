package com.xmlcalabash.exceptions

import java.net.URI
import com.jafpl.exceptions.{JafplException, JafplExceptionCode}
import com.jafpl.graph.Location
import com.jafpl.messages.{Message, Metadata}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.Artifact
import com.xmlcalabash.runtime.{StaticContext, XProcExpression}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode}

import scala.collection.mutable.ListBuffer

object XProcException {
  val xd0011 = new QName("err", XProcConstants.ns_err, "XD0011")
  val xd0038 = new QName("err", XProcConstants.ns_err, "XD0038")
  val xd0072 = new QName("err", XProcConstants.ns_err, "XD0072")

  val xc0070 = new QName("err", XProcConstants.ns_err, "XC0070")
  val xs0107 = new QName("err", XProcConstants.ns_err, "XS0107")

  def xtde(errNo: Int): StructuredQName = {
    new StructuredQName("err", XProcConstants.ns_xqt_errors, f"XTDE$errNo%04d")
  }

  def xtte(errNo: Int): StructuredQName = {
    new StructuredQName("err", XProcConstants.ns_xqt_errors, f"XTTE$errNo%04d")
  }

  def xiUnkExprType(location: Option[Location]): XProcException = internalError(1, location)
  def xiInvalidMessage(location: Option[Location], message: Message): XProcException = internalError(2, location, message)
  def xiBadBoundValue(location: Option[Location], value: Any): XProcException = internalError(3, location, value)
  def xiUnexpectedExprType(location: Option[Location], expr: Any): XProcException = internalError(4, location, expr)
  def xiSeqNotSupported(location: Option[Location], expr: XProcExpression): XProcException = internalError(5, location, expr)
  def xiInvalidClarkName(location: Option[Location], name: String): XProcException = internalError(6, location, name)
  def xiInvalidMetadata(location: Option[Location], metadata: Metadata): XProcException = internalError(7, location, metadata)
  def xiExtFunctionNotAllowed(): XProcException = internalError(8, None)
  def xiInvalidAVT(location: Option[Location], expr: String): XProcException = internalError(9,location, expr)
  def xiParamsNotMap(location: Option[Location], props: Any): XProcException = internalError(10, location, props)
  def xiDocPropsUnavail(location: Option[Location], baseURI: URI): XProcException = internalError(11, location, baseURI)
  def xiDocPropsNotMap(location: Option[Location], props: Any): XProcException = internalError(12, location, props)
  def xiDocPropsKeyNotString(location: Option[Location], key: Any): XProcException = internalError(13, location, key)
  def xiDocPropsValueNotAtomic(location: Option[Location], key: Any): XProcException = internalError(14, location, key)
  def xiNotInInjectable(): XProcException = internalError(15, None)
  def xiNoBindingForPort(port: String): XProcException = internalError(16, None, port)
  def xiInjectMessageNodes(location: Option[Location]): XProcException = internalError(17, location)
  def xiInjectRedefPort(location: Option[Location]): XProcException = internalError(18, location)
  def xiChildNotFound(location: Option[Location]): XProcException = internalError(19, location)
  def xiBadPatch(node: Artifact, location: Option[Location]): XProcException = internalError(20, location, node)
  def xiBadPatchChild(node: Artifact, location: Option[Location]): XProcException = internalError(21, location, node)
  def xiBadMessage(message: Message, location: Option[Location]): XProcException = internalError(22, location, message)
  def xiInvalidPort(port: String, location: Option[Location]): XProcException = internalError(23, location, port)
  def xiInvalidPropertyValue(value: Any, location: Option[Location]): XProcException = internalError(24, location, value)
  def xiRedefId(id: String, location: Option[Location]): XProcException = internalError(25, location, id)
  def xiMergeBadRoot(root: QName, location: Option[Location]): XProcException = internalError(26, location, root)
  def xiMergeBadText(text: String, location: Option[Location]): XProcException = internalError(27, location, text)
  def xiMergeBadValue(value: Any, location: Option[Location]): XProcException = internalError(28, location, value)
  def xiMergeDup(key: QName, location: Option[Location]): XProcException = internalError(29, location, key)
  def xiMergeXsiTypeOnNode(location: Option[Location]): XProcException = internalError(30, location)
  def xiMergeBadAtomic(ptype: QName, location: Option[Location]): XProcException = internalError(31, location, ptype)
  def xiWrapItems(location: Option[Location]): XProcException = internalError(32, location)
  def xiWrapXML(location: Option[Location]): XProcException = internalError(33, location)
  def xiCastXML(value: Any, location: Option[Location]): XProcException = internalError(34, location, value)
  def xiMediaType(value: Any, location: Option[Location]): XProcException = internalError(35, location, value)
  def xiArgBundleNoPipeline(): XProcException = internalError(36, None)
  def xiArgBundleRedefined(name: QName): XProcException = internalError(37, None, name)
  def xiArgBundlePfxChar(str: String): XProcException = internalError(38, None, str)
  def xiArgBundleCannotParseInput(str: String): XProcException = internalError(39, None, str)
  def xiArgBundleCannotParseGraphType(str: String): XProcException = internalError(39, None, str)
  def xiArgBundleCannotParseOutput(str: String): XProcException = internalError(40, None, str)
  def xiArgBundleRedefinedNamespace(pfx: String): XProcException = internalError(41, None, pfx)
  def xiArgBundleCannotParseNamespace(str: String): XProcException = internalError(42, None, str)
  def xiArgBundleUnexpectedOption(str: String): XProcException = internalError(43, None, str)
  def xiArgBundleIndexOOB(str: String): XProcException = internalError(44, None, str)
  def xiArgBundleMultiplePipelines(p1: String, p2: String): XProcException = internalError(45, None, List(p1, p2))
  def xiArgBundleInvalidPortSpec(spec: String): XProcException = internalError(46, None, spec)
  def xiNotAnXPathExpression(expr: Any, location: Option[Location]): XProcException = internalError(47, location, expr)
  def xiNotXMLCalabash(): XProcException = internalError(48, None)
  def xiDifferentXMLCalabash(): XProcException = internalError(49, None)
  def xiNodesNotAllowed(node: XdmNode): XProcException = internalError(50, None, node)
  def xiWrongImplParams(): XProcException = internalError(51, None)
  def xiNoSuchPortOnAccept(port: String): XProcException = internalError(52, None, List(port))
  def xiBadValueOnFileLoader(variable: String): XProcException = internalError(53, None, List(variable))
  def xiNoSaxon(): XProcException = internalError(54, None, None)
  def xiNotADocument(location: Option[Location]): XProcException = internalError(55, location, None)
  def xiNotATextDocument(location: Option[Location]): XProcException = internalError(56, location, None)
  def xiNotAnXmlDocument(location: Option[Location]): XProcException = internalError(57, location, None)
  def xiNotJSON(location: Option[Location]): XProcException = internalError(58, location, None)
  def xiNotBinary(location: Option[Location]): XProcException = internalError(59, location, None)
  def xiUnexpectedItem(kind: String, location: Option[Location]): XProcException = internalError(60, location, kind)
  def xiMaxRegressionsExceededWhile(max: Long, location: Option[Location]): XProcException = internalError(61, location, max)
  def xiMaxRegressionsExceededUntil(max: Long, location: Option[Location]): XProcException = internalError(62, location, max)
  def xiAttempToCountByZero(location: Option[Location]): XProcException = internalError(63, location)
  def xiBadConfigSchema(scheme: String): XProcException = internalError(64, None, scheme)
  def xiConfigNotFound(cfg: URI): XProcException = internalError(65, None, cfg)
  def xiMalformedURI(uri: String, message: String, location: Option[Location]): XProcException = internalError(66, location, List(uri, message))
  def xiArgBundleDuplicateLogLevel(level: String, curLevel: String): XProcException = internalError(67, None, List(level, curLevel))
  def xiArgBundleAmbiguousGraphKey(token: String): XProcException = internalError(68, None, token)
  def xiArgBundleInvalidGraphKey(token: String): XProcException = internalError(69, None, token)

  def xiThisCantHappen(msg: String, location: Option[Location]): XProcException = internalError(999, location, msg)

  def xdContextItemSequence(expr: String, msg: String, location: Option[Location]): XProcException = dynamicError(1, List(expr, msg), location)
  def xdSequenceNotAllowedOnIf(location: Option[Location]): XProcException = dynamicError(5, location)
  def xdInputSequenceNotAllowed(port: String, location: Option[Location]): XProcException = dynamicError(6, port, location)
  def xdOutputSequenceNotAllowed(port: String, location: Option[Location]): XProcException = dynamicError(7, port, location)
  def xdSequenceNotAllowedAsContext(location: Option[Location]): XProcException = dynamicError(8, location)
  def xdViewportOnAttribute(pattern: String, location: Option[Location]): XProcException = dynamicError(10, pattern, location)
  def xdDoesNotExist(href: String, location: Option[Location]): XProcException = dynamicError(11, href, location)
  def xdCannotResolveQName(name: String, location: Option[Location]): XProcException = dynamicError((15,1), name, location)
  def xdMissingNamespaceBinding(msg: String, location: Option[Location]): XProcException = dynamicError((15,2), msg, location)
  def xdInvalidSelection(expr: String, selected: String, location: Option[Location]): XProcException = dynamicError(16, List(expr,selected), location)

  def xdContextItemAbsent(expr: String, msg: String, location: Option[Location]): XProcException = dynamicError(1, List(expr, msg), location)

  // FIXME: subtypes
  def xdBadValue(value: String, vtype: String, location: Option[Location]): XProcException = dynamicError((19, 1), List(value,vtype), location)
  def xdBadMatchPattern(pattern: String, message: String, location: Option[Location]): XProcException = dynamicError((19, 2), List(pattern, message), location)
  def xdBadVisibility(visibility: String, location: Option[Location]): XProcException = dynamicError((19, 3), visibility, location)
  def xdBadValue(value: String, location: Option[Location]): XProcException = dynamicError((19, 4), value, location)
  def xdValueNotInList(value: String, values: String, location: Option[Location]): XProcException = dynamicError((19,5), List(value, values), location)

  def xdNotAuthorized(href: String, message: String, location: Option[Location]): XProcException = dynamicError(21, List(href, message), location)
  def xdGeneralError(message: String, location: Option[Location]): XProcException = dynamicError(30, message, location)

  def xdNotValidXML(href: String, message: String, location: Option[Location]): XProcException = dynamicError(23, List(href, message), location)
  def xdNotValidXML(href: String, line: Long, col: Long, message: String, location: Option[Location]): XProcException = dynamicError(23, List(href, line, col, message), location)
  def xdValueDoesNotSatisfyType(value: String, location: Option[Location]): XProcException = dynamicError(28, value, location)

  def xdStepFailed(msg: String, location: Option[Location]): XProcException = dynamicError(30, msg, location)
  def xdConflictingNamespaceDeclarations(msg: String, location: Option[Location]): XProcException = dynamicError(34, msg, location)
  def xdBadType(value: String, as: String, location: Option[Location]): XProcException = dynamicError(36, List(value, as), location)
  def xdBadType(name: QName, value: String, as: String, location: Option[Location]): XProcException = dynamicError(36, List(name, value, as), location)
  def xdBadInputMediaType(ctype: MediaType, allowed: List[MediaType], location: Option[Location]): XProcException = dynamicError(38, List(ctype, allowed), location)
  def xdUnsupportedCharset(charset: String, location: Option[Location]): XProcException = dynamicError(39, charset, location)
  def xdIncorrectEncoding(encoding: String, location: Option[Location]): XProcException = dynamicError(40, encoding, location)
  def xdBadOutputMediaType(ctype: MediaType, allowed: List[MediaType], location: Option[Location]): XProcException = dynamicError(42, List(ctype, allowed), location)
  def xdNotWFXML(href: String, message: String, location: Option[Location]): XProcException = dynamicError(49, List(href, message), location)
  def xdNotWFXML(href: String, line: Long, col: Long, message: String, location: Option[Location]): XProcException = dynamicError(49, List(href, line, col, message), location)

  def xdCannotEncodeMarkup(encoding: String, contentType: MediaType, location: Option[Location]): XProcException = dynamicError(54, List(encoding,contentType), location)
  def xdCharsetWithoutEncoding(contentType: String, location: Option[Location]): XProcException = dynamicError(55, contentType, location)
  def xdNoMarkupAllowedEncoded(name: QName, location: Option[Location]): XProcException = dynamicError(56, name, location)
  def xdInvalidJson(message: String, location: Option[Location]): XProcException = dynamicError(57, message, location)
  def xdUnsupportedEncoding(encoding: String, location: Option[Location]): XProcException = dynamicError(60, encoding, location)
  def xdKeyIsInvalidQName(key: String, location: Option[Location]): XProcException = dynamicError(61, key, location)
  def xdMismatchedContentType(declType: MediaType, propType: MediaType, location: Option[Location]): XProcException = dynamicError(62, List(declType,propType), location)
  def xdNoMarkupAllowed(name: QName, location: Option[Location]): XProcException = dynamicError(63, name, location)
  def xdInvalidURI(uri: String, location: Option[Location]): XProcException = dynamicError(64, uri, location)
  def xdBadMapKey(key: String, location: Option[Location]): XProcException = dynamicError(70, key, location)
  def xdBadViewportInput(contentType: MediaType, location: Option[Location]): XProcException = dynamicError(72, contentType, location)
  def xdBadViewportResult(location: Option[Location]): XProcException = dynamicError(73, location)

  def xdUrifyFailed(filepath: String, basedir: String, location: Option[Location]): XProcException = dynamicError(74, List(filepath, basedir), location)
  def xdUrifyDifferentDrives(filepath: String, basedir: String, location: Option[Location]): XProcException = dynamicError(75, List(filepath, basedir), location)
  def xdUrifyMixedDrivesAndAuthorities(filepath: String, basedir: String, location: Option[Location]): XProcException = dynamicError(76, List(filepath, basedir), location)
  def xdUrifyDifferentSchemes(filepath: String, basedir: String, location: Option[Location]): XProcException = dynamicError(77, List(filepath, basedir), location)

  def xdUnrecognizedContentType(ctype: String, location: Option[Location]): XProcException = dynamicError(79, ctype, location)

  def xdUrifyNonhierarchicalBase(filepath: String, basedir: String, location: Option[Location]): XProcException = dynamicError(80, List(filepath, basedir), location)

  def xsLoop(step: String, port: String, location: Option[Location]): XProcException = staticError(1, List(step, port), location)
  def xsDuplicateStepName(step: String, location: Option[Location]): XProcException = staticError(2, step, location)
  def xsUnconnectedInputPort(step: String, port: String, location: Option[Location]): XProcException = staticError(3, List(step,port), location)
  def xsDupOptionName(location: Option[Location], name: String): XProcException = staticError(4, name, location)
  def xsUnconnectedOutputPort(step: String, port: String, location: Option[Location]): XProcException = staticError(6, List(step, port), location)
  def xsBadAttribute(name: QName, location: Option[Location]): XProcException = staticError(8, name, location)
  def xsBadPortName(stepType: QName, port: String, location: Option[Location]): XProcException = staticError(10, List(stepType, port), location)
  def xsDupPortName(port: String, location: Option[Location]): XProcException = staticError(11, port, location)
  def xsMissingRequiredOption(optName: QName, location: Option[Location]): XProcException = staticError(18, optName, location)

  def xsPortNotReadableNoStep(step: String, location: Option[Location]): XProcException = staticError((22,1), step, location)
  def xsPortNotReadableNoPrimaryInput(step: String, location: Option[Location]): XProcException = staticError((22,2), step, location)
  def xsPortNotReadableNoPrimaryOutput(step: String, location: Option[Location]): XProcException = staticError((22,3), step, location)
  def xsPortNotReadable(step: String, port: String, location: Option[Location]): XProcException = staticError((22,4), List(step, port), location)

  def xsOptionInXProcNamespace(name: QName, location: Option[Location]): XProcException = staticError(28, name, location)

  def xsDupPrimaryPort(port: String, primaryPort: String, location: Option[Location]): XProcException = staticError(30, List(port, primaryPort), location)
  def xsUndeclaredOption(stepType: QName, optName: QName, location: Option[Location]): XProcException = staticError(31, List(stepType,optName), location)
  def xsUnconnectedPrimaryInputPort(step: String, port: String, location: Option[Location]): XProcException = staticError(32, List(step,port), location)
  def xsDupStepType(stepType: QName, location: Option[Location]): XProcException = staticError(36, stepType, location)
  def xsTextNotAllowed(text: String, location: Option[Location]): XProcException = staticError(37, text, location)
  def xsMissingRequiredAttribute(attName: QName, location: Option[Location]): XProcException = staticError(38, attName, location)
  def xsPortNotAllowed(port: String, step: String, location: Option[Location]): XProcException = staticError(43, List(port,step), location)
  def xsMissingDeclaration(name: QName, location: Option[Location]): XProcException = staticError(44, name, location)

  def xsImportFailed(href: URI, location: Option[Location]): XProcException = staticError((52,1), href, location)
  def xsBadImport(name: QName, location: Option[Location]): XProcException = staticError((52,2), name, location)
  def xsStepTypeRequired(location: Option[Location]): XProcException = staticError(53, List(), location)
  def xsInvalidVersion(version: Double, location: Option[Location]): XProcException = staticError(60, version, location)
  def xsVersionRequired(location: Option[Location]): XProcException = staticError(62, location)
  def xsBadVersion(version: String, location: Option[Location]): XProcException = staticError(63, version, location)
  def xsCatchMissingCode(location: Option[Location]): XProcException = staticError((64,1), location)
  def xsCatchBadCode(code: QName, location: Option[Location]): XProcException = staticError((64,2), code, location)
  def xsCatchRepeatedCode(code: QName, location:Option[Location]): XProcException = staticError((64,3), code, location)
  def xsNoPrimaryInputPort(stepType: QName, location: Option[Location]): XProcException = staticError(65, stepType, location)

  def xsPipeWithoutStepOrDrp(location: Option[Location]): XProcException = staticError((67,1), location)
  def xsPipeWithoutPortOrPrimaryOutput(location: Option[Location]): XProcException = staticError((67,2), location)

  def xsUnsupportedEncoding(encoding: String, location: Option[Location]): XProcException = staticError(69, encoding, location)
  def xsInvalidFinallyPortName(port: String, location: Option[Location]): XProcException = staticError(72, port, location)
  def xsNotAStep(name: String, location: Option[Location]): XProcException = staticError(73, name, location)
  def xsMissingWhen(location: Option[Location]): XProcException = staticError(74, location)
  def xsInvalidTryCatch(msg: String, location: Option[Location]): XProcException = staticError(75, msg, location)

  def xsBadTypeValue(value: String, reqdType: String, location: Option[Location]): XProcException = staticError(77, List(value, reqdType), location)

  def xsInlineCommentNotAllowed(comment: String, location: Option[Location]): XProcException = staticError((79,1), comment, location)
  def xsInlinePiNotAllowed(pi: String, location: Option[Location]): XProcException = staticError((79,2), pi, location)
  def xsInlineTextNotAllowed(text: String, location: Option[Location]): XProcException = staticError((79,3), text, location)

  def xsDupWithOptionName(optName: QName, location: Option[Location]): XProcException = staticError(80, optName, location)

  def xsHrefAndOtherSources(location: Option[Location]): XProcException = staticError(81, location)
  def xsPipeAndOtherSources(location: Option[Location]): XProcException = staticError(82, location)
  def xsCatchInvalidCode(code: String, location:Option[Location]): XProcException = staticError(83, code, location)
  def xsInlineExpandTextNotAllowed(location: Option[Location]): XProcException = staticError(84, location)
  def xsPipeAndHref(location: Option[Location]): XProcException = staticError(85, location)
  def xsDupWithInputPort(port: String, location: Option[Location]): XProcException = staticError(86, port, location)

  def xsTvtForbidden(location: Option[Location]): XProcException = staticError(88, location)
  def xsNoSiblingsOnEmpty(location: Option[Location]): XProcException = staticError(89, None, location)
  def xsInvalidPipeToken(token: String, location: Option[Location]): XProcException = staticError(90, token, location)

  def xsNoSelectOnStaticOption(location: Option[Location]): XProcException = staticError(93, None, location)
  def xsNoSelectOnVariable(location: Option[Location]): XProcException = staticError(94, None, location)
  def xsInvalidSequenceType(seqType: String, errMsg: String, location: Option[Location]): XProcException = staticError(96, List(seqType, errMsg), location)
  def xsXProcNamespaceError(attrName: QName, location: Option[Location]): XProcException = staticError(97, attrName, location)

  //def xsElementNotAllowed(element: QName, message: String, location: Option[Location]): XProcException = staticError(100, List(element, message), location)

  def xsInvalidPipeline(message: String, location: Option[Location]): XProcException = staticError((100,1), message, location)
  def xsElementNotAllowed(element: QName, location: Option[Location]): XProcException = staticError((100,2), List(element, "element is not allowed here"), location)
  def xsInvalidValues(values: String, location: Option[Location]): XProcException = staticError(101, values, location)
  def xsBadChooseOutputs(primary: String, alsoPrimary: String, location: Option[Location]): XProcException = staticError((102,1), List(primary,alsoPrimary), location)
  def xsBadTryOutputs(primary: String, alsoPrimary: String, location: Option[Location]): XProcException = staticError((102,2), List(primary,alsoPrimary), location)

  def xsMissingRequiredInput(port: String, location: Option[Location]): XProcException = staticError(998, port, location)

  def xsXProcElementNotAllowed(name: String, location: Option[Location]): XProcException = staticError(100, name, location)

  def xsNoBindingInExpression(name: QName, location: Option[Location]): XProcException = staticError((107,1), name, location)
  def xsStaticErrorInExpression(expr: String, msg: String, location: Option[Location]): XProcException = staticError((107,2), List(expr, msg), location)
  def xsStaticRefsContext(msg: String, location: Option[Location]): XProcException = staticError((107,3), msg, location)
  def xsStaticRefsNonStatic(name: QName, location: Option[Location]): XProcException = staticError((107,4), name, location)
  def xsStaticRefsNonStaticStr(name: String, location: Option[Location]): XProcException = staticError((107,4), name, location)

  def xsPrimaryOutputRequired(location: Option[Location]): XProcException = staticError(108, location)
  def xsUnrecognizedContentTypeShortcut(ctype: String, location: Option[Location]): XProcException = staticError(111, ctype, location)
  def xsPrimaryOutputOnFinally(port: String, location: Option[Location]): XProcException = staticError(112, port, location)

  def xcGeneralException(code: QName, errors: Option[XdmNode], location: Option[Location]): XProcException = {
    val except = new XProcException(code, 1, None, location, List())
    if (errors.isDefined) {
      except.errors = errors.get
    }
    except
  }

  def xcGeneralException(code: QName, cause: Exception, errors: Option[XdmNode], location: Option[Location]): XProcException = {
    val except = new XProcException(code, 1, None, location, List())
    if (errors.isDefined) {
      except.errors = errors.get
    }
    except.underlyingCauses = List(cause)
    except
  }

  def xcContentTypeIsNotText(contentType: String, location: Option[Location]): XProcException = stepError(1, contentType, location)
  def xcHttpBadAuth(msg: String, location: Option[Location]): XProcException = stepError(3, msg, location)
  def xcXsltInvalidParameterType(name: QName, typename: String, location: Option[Location]): XProcException = stepError(7, List(name, typename), location)
  def xcXsltNoMode(mode: QName, msg: String, location: Option[Location]): XProcException = stepError(8, List(mode, msg), location)
  def xcXQueryVersionNotAvailable(version: String, location: Option[Location]): XProcException = stepError(9, version, location)
  def xcBadRenamePI(name: QName, location: Option[Location]): XProcException = stepError(13, name, location)
  def xcInvalidSelection(pattern: String, nodeType: String, location: Option[Location]): XProcException = stepError(23, List(pattern, nodeType), location)
  def xcBadPosition(pattern: String, position: String, location: Option[Location]): XProcException = stepError(24, List(pattern, position), location)
  def xcBadChildPosition(pattern: String, position: String, location: Option[Location]): XProcException = stepError(25, List(pattern, position), location)
  def xcXIncludeError(msg: String, location: Option[Location]): XProcException = stepError((29,1), msg, location)
  def xcXIncludeError(href: String, msg: String, location: Option[Location]): XProcException = stepError((29,2), List(href, msg), location)
  def xcHttpCantInterpret(msg: String, location: Option[Location]): XProcException = stepError(30, msg, location)

  def xcOsExecBadSeparator(pathSeparator: String, location: Option[Location]): XProcException = stepError(33, pathSeparator, location)
  def xcOsExecBadCwd(path: String, location: Option[Location]): XProcException = stepError(34, path, location)
  def xcBadCrcVersion(version: String, location: Option[Location]): XProcException = stepError((36,1), version, location)
  def xcBadMdVersion(version: String, location: Option[Location]): XProcException = stepError((36,2), version, location)
  def xcBadShaVersion(version: String, location: Option[Location]): XProcException = stepError((36,3), version, location)
  def xcBadHashAlgorithm(algorithm: String, location: Option[Location]): XProcException = stepError((36,4), algorithm, location)
  def xcHashFailed(message: String, location: Option[Location]): XProcException = stepError((36,5), message, location)
  def xcMissingHmacKey(location: Option[Location]): XProcException = stepError((36,6), location)
  def xcValueNotFormUrlEncoded(value: String, location: Option[Location]): XProcException = stepError(37, value, location)
  def xcVersionNotAvailable(version: String, location: Option[Location]): XProcException = stepError(38, version, location)

  def xcCannotStore(href: URI, location: Option[Location]): XProcException = stepError(50, href, location)
  def xcNotSchemaValidNvdl(href: String, message: String, location: Option[Location]): XProcException = stepError(53, List(href, message), location)
  def xcNotSchemaValidSchematron(href: URI, message: String, location: Option[Location]): XProcException = stepError(54, List(href, message), location)
  def xcXsltNoTemplate(template: QName, location: Option[Location]): XProcException = stepError(56, template, location)
  def xcCannotAddNamespaces(name: QName, location: Option[Location]): XProcException = stepError(59, name, location)
  def xcUnsupportedUuidVersion(version: Integer, location: Option[Location]): XProcException = stepError(60, version.toString, location)
  def xcOsExecFailed(rc: Int, location: Option[Location]): XProcException = stepError(64, rc.toString, location)
  def xcContentTypeNotAllowed(location: Option[Location]): XProcException = stepError(69, location)
  def xcSerializationNotAMap(value: String, location: Option[Location]): XProcException = stepError(70, value, location)
  def xcInvalidBase64(message: String, location: Option[Location]): XProcException = stepError(72, message, location)
  def xcContentTypeMissing(location: Option[Location]): XProcException = stepError(73, location)
  def xcDifferentContentTypes(castTo: String, cdata: String, location: Option[Location]): XProcException = stepError(74, List(castTo,cdata), location)
  def xcArchiveInvalidParameterValue(parameter: String, value: String, location: Option[Location]): XProcException = stepError(79, List(parameter, value), location)
  def xcArchiveTooManyArchives(location: Option[Location]): XProcException = stepError((80,1), location)
  def xcArchiveTooFewArchives(location: Option[Location]): XProcException = stepError((80,2), location)
  def xcArchiveBadURI(uri: URI, location: Option[Location]): XProcException = stepError((84,1), uri, location)
  def xcArchiveBadURI(location: Option[Location]): XProcException = stepError((84,2), location)
  def xcUnrecognizedArchiveFormat(location: Option[Location]): XProcException = stepError((85,1), location)
  def xcUnknownArchiveFormat(format: QName, location: Option[Location]): XProcException = stepError((85,2), format, location)
  def xcArchiveFormatError(format: QName, location: Option[Location]): XProcException = stepError((85,3), format, location)
  def xcMultipleTopLevelElements(location: Option[Location]): XProcException = stepError(91, location)
  def xcAttributeNameCollision(qname: QName, location: Option[Location]): XProcException = stepError(92, qname, location)
  def xcXsltCompileError(msg: String, ex: Exception, location: Option[Location]): XProcException = stepError(93, msg, location)
  def xcXsltInputNot20Compatible(media: MediaType, location: Option[Location]): XProcException = stepError(94, media, location)
  def xcXsltRuntimeError(code: QName, msg: String, location: Option[Location]): XProcException = stepError(95, List(code, msg), location)
  def xcXsltUserTermination(msg: String, location: Option[Location]): XProcException = stepError(96, msg, location)
  def xcSortError(msg: String, location: Option[Location]): XProcException = stepError(98, msg, location)
  def xcSortKeyError(location: Option[Location]): XProcException = stepError(99, location)
  def xcArchiveBadManifest(location: Option[Location]): XProcException = stepError(100, location)
  def xcXQueryInputNot30Compatible(media: MediaType, location: Option[Location]): XProcException = stepError(101, media, location)
  def xcXQueryInvalidParameterType(name: QName, typename: String, location: Option[Location]): XProcException = stepError(102, List(name, typename), location)
  def xcXQueryCompileError(msg: String, location: Option[Location]): XProcException = stepError(103, msg, location)
  def xcXQueryEvalError(msg: String, location: Option[Location]): XProcException = stepError(104, msg, location)
  def xcRejectDuplicateKeys(key: String, location: Option[Location]): XProcException = stepError(106, key, location)
  def xcPrefixNotInScope(prefix: String, location: Option[Location]): XProcException = stepError(108, prefix, location)
  def xcNamespaceDeleteCollision(uri: String, location: Option[Location]): XProcException = stepError(109, uri, location)
  def xcInvalidJsonMergeKey(key: String, location: Option[Location]): XProcException = stepError((110,1), key, location)
  def xcInvalidJsonMergeKey(location: Option[Location]): XProcException = stepError((110,2), location)
  def xcArchiveTooManyManifests(location: Option[Location]): XProcException = stepError(112, location)
  def xcFileDeleteNotRecursive(href: URI, location: Option[Location]): XProcException = stepError(113, href, location)
  def xcFileMkdirFail(href: URI, location: Option[Location]): XProcException = stepError(114, href, location)
  def xcFileMoveOverwriteForbidden(href: URI, target: URI, location: Option[Location]): XProcException = stepError(115, List(href, target), location)
  def xcInvalidFlattenValue(value: String, location: Option[Location]): XProcException = stepError(119, value, location)
  def xcArchiveNoBaseURI(location: Option[Location]): XProcException = stepError(120, location)

  def xcHttpInvalidAuth(name: String, value: String, location: Option[Location]): XProcException = stepError(123, List(name, value), location)
  def xcHttpInvalidParameter(name: String, value: String, location: Option[Location]): XProcException = stepError((124,1), List(name, value), location)
  def xcHttpInvalidParameterType(name: String, value: String, location: Option[Location]): XProcException = stepError((124,2), List(name, value), location)
  def xcHttpMultipartForbidden(href: URI, location: Option[Location]): XProcException = stepError(125, href, location)
  def xcHttpDuplicateHeader(name: String, location: Option[Location]): XProcException = stepError(127, name, location)
  def xcHttpUnsupportedScheme(href: URI, location: Option[Location]): XProcException = stepError(128, href, location)
  def xcHttpUnsupportedHttpVersion(version: String, location: Option[Location]): XProcException = stepError(129, version, location)
  def xcUnsupportedTransferEncoding(encoding: String, location: Option[Location]): XProcException = stepError(131, encoding, location)
  def xcMultipartRequired(contentType: String, location: Option[Location]): XProcException = stepError(133, contentType, location)
  def xcHttpAssertFailed(assert: String, location: Option[Location]): XProcException = stepError(126, assert, location)

  def xcFileInfoBadScheme(uri: URI, location: Option[Location]): XProcException = stepError(134, uri, location)
  def xcFileTouchBadScheme(uri: URI, location: Option[Location]): XProcException = stepError(136, uri, location)
  def xcFileCreateTempFileBadScheme(uri: URI, location: Option[Location]): XProcException = stepError(138, uri, location)

  def xcFileMkdirBadScheme(uri: URI, location: Option[Location]): XProcException = stepError(140, uri, location)
  def xcFileDeleteBadScheme(uri: URI, location: Option[Location]): XProcException = stepError(142, uri, location)
  def xcFileDeleteNotAllowed(uri: URI, location: Option[Location]): XProcException = stepError(143, uri, location)

  def xcFileCopyBadScheme(uri: URI, location: Option[Location]): XProcException = stepError(144, uri, location)
  def xcOverrideContentTypesMalformed(location: Option[Location]): XProcException = stepError(146, location)
  def xcOverrideContentTypesBadRegex(regex: String, location: Option[Location]): XProcException = stepError(147, regex, location)
  def xcFileMoveBadScheme(uri: URI, location: Option[Location]): XProcException = stepError(148, uri, location)
  def xcNotASchematronDocument(): XProcException = stepError(151, None)
  def xcNotSchemaValidRelaxNG(href: String, message: String, location: Option[Location]): XProcException = stepError(155, List(href, message), location)
  def xcNotSchemaValidXmlSchema(href: String, message: String, location: Option[Location]): XProcException = stepError((156,1), List(href, message), location)
  def xcNotSchemaValidXmlSchema(href: String, line: Long, col: Long, message: String, location: Option[Location]): XProcException = stepError((156,2), List(href, line, col, message), location)
  def xcFileCopyDirToFile(source: URI, target: URI, location: Option[Location]): XProcException = stepError(157, List(source, target), location)
  def xcFileMoveDirToFile(source: URI, target: URI, location: Option[Location]): XProcException = stepError(158, List(source, target), location)

  def xcInvalidResultDataFormat(location: Option[Location]): XProcException = stepError(201, location)
  def xcUnknownCompressionFormat(location: Option[Location]): XProcException = stepError((202,1), location)
  def xcUnknownCompressionFormat(format: QName, location: Option[Location]): XProcException = stepError((202,2), format, location)
  def xcUncompressionError(location: Option[Location]): XProcException = stepError((202,3), location)

  def xcHttpInvalidBoundary(boundary: String, location: Option[Location]): XProcException = stepError(203, boundary, location)

  def xcXIncludeFallbackPlacement(location: Option[Location]): XProcException = stepError(999, location)
  def xcXIncludeMultipleFallback(location: Option[Location]): XProcException = stepError(999, location)
  def xcXIncludeInvalidAccept(accept: String, location: Option[Location]): XProcException = stepError(999, accept, location)
  def xcXIncludeInvalidAcceptLang(accept: String, location: Option[Location]): XProcException = stepError(999, accept, location)
  def xcXIncludeInvalidElement(name: QName, location: Option[Location]): XProcException = stepError(999, name, location)
  def xcXPointerError(msg: String): XProcException = stepError(999, msg, None)
  def xcUnparseableXPointer(xptr: String): XProcException = stepError(999, xptr, None)
  def xcXPointerMalformedSearch(select: String, msg: String): XProcException = stepError(999, List(select, msg), None)
  def xcXPointerNotFound(select: String): XProcException = stepError(999, select, None)
  def xcXPointerUnparseableXmlnsScheme(data: String): XProcException = stepError(999, data, None)
  def xcXPointerUnparseableElementScheme(data: String): XProcException = stepError(999, data, None)
  def xcInvalidTrim(trim: String): XProcException = stepError(999, trim, None)
  def xcXIncludeLoop(href: String, location: Option[Location]): XProcException = stepError(29, href, location)
  def xcXIncludeResourceError(href: String, location: Option[Location]): XProcException = stepError(29, href, location)

  def xxUnmappedException(ex: JafplException, details: List[Any]): XProcException = extensionError(999, List(ex.getMessage(), details))
  def xxInvalidLoop(details: List[Any]): XProcException = extensionError(60, details)

  def staticErrorCode(code: Int): QName = {
    new QName("err", XProcConstants.ns_err, "XS%04d".format(code))
  }

  def dynamicErrorCode(code: Int): QName = {
    new QName("err", XProcConstants.ns_err, "XD%04d".format(code))
  }

  def stepErrorCode(code: Int): QName = {
    new QName("err", XProcConstants.ns_err, "XC%04d".format(code))
  }

  def extensionErrorCode(code: Int): QName = {
    new QName("cxerr", XProcConstants.ns_cxerr, "XX%04d".format(code))
  }

  private def internalError(code: Int, location: Option[Location]): XProcException = {
    internalError(code, location, List())
  }

  private def internalError(code: Int, location: Option[Location], args: Any): XProcException = {
    internalError(code, location, List(args))
  }

  private def internalError(code: Int, location: Option[Location], args: List[Any]): XProcException = {
    val qname = new QName("cx", XProcConstants.ns_cx, "XI%04d".format(code))
    new XProcException(qname, 1, None, location, args)
  }

  // ====================================================================================

  private def dynamicError(code: (Int, Int), details: List[Any], location: Option[Location]): XProcException = {
    val qname = dynamicErrorCode(code._1)
    new XProcException(qname, code._2, None, location, details)
  }

  private def dynamicError(code: Int, details: List[Any], location: Option[Location]): XProcException = {
    dynamicError((code, 1), details, location)
  }

  private def dynamicError(code: (Int, Int), details: Any, location: Option[Location]): XProcException = {
    dynamicError(code, List(details), location)
  }

  private def dynamicError(code: Int, details: Any, location: Option[Location]): XProcException = {
    dynamicError((code, 1), List(details), location)
  }

  private def dynamicError(code: (Int, Int), location: Option[Location]): XProcException = {
    dynamicError(code, List(), location)
  }

  private def dynamicError(code: Int, location: Option[Location]): XProcException = {
    dynamicError((code, 1), List(), location)
  }

  // ====================================================================================

  private def staticError(code: (Int, Int), details: List[Any], location: Option[Location]): XProcException = {
    val qname = staticErrorCode(code._1)
    new XProcException(qname, code._2, None, location, details)
  }

  private def staticError(code: Int, details: List[Any], location: Option[Location]): XProcException = {
    staticError((code, 1), details, location)
  }

  private def staticError(code: (Int, Int), details: Any, location: Option[Location]): XProcException = {
    staticError(code, List(details), location)
  }

  private def staticError(code: Int, details: Any, location: Option[Location]): XProcException = {
    staticError((code, 1), List(details), location)
  }

  private def staticError(code: (Int, Int), location: Option[Location]): XProcException = {
    staticError(code, List(), location)
  }

  private def staticError(code: Int, location: Option[Location]): XProcException = {
    staticError((code, 1), List(), location)
  }

  // ====================================================================================

  private def stepError(code: (Int, Int), details: List[Any], location: Option[Location]): XProcException = {
    val qname = stepErrorCode(code._1)
    new XProcException(qname, code._2, None, location, details)
  }

  private def stepError(code: Int, details: List[Any], location: Option[Location]): XProcException = {
    stepError((code, 1), details, location)
  }

  private def stepError(code: (Int, Int), details: Any, location: Option[Location]): XProcException = {
    stepError(code, List(details), location)
  }

  private def stepError(code: Int, details: Any, location: Option[Location]): XProcException = {
    stepError((code, 1), List(details), location)
  }

  private def stepError(code: (Int, Int), location: Option[Location]): XProcException = {
    stepError(code, List(), location)
  }

  private def stepError(code: Int, location: Option[Location]): XProcException = {
    stepError((code, 1), List(), location)
  }

  // ====================================================================================

  private def extensionError(code: (Int, Int), details: List[Any]): XProcException = {
    val qname = extensionErrorCode(code._1)
    new XProcException(qname, code._2, None, None, details)
  }

  private def extensionError(code: Int, details: List[Any]): XProcException = {
    extensionError((code, 1), details)
  }

  private def extensionError(code: (Int, Int), details: Any): XProcException = {
    extensionError(code, List(details))
  }

  private def extensionError(code: Int, details: Any): XProcException = {
    extensionError((code, 1), List(details))
  }

  private def extensionError(code: (Int, Int)): XProcException = {
    extensionError(code, List())
  }

  private def extensionError(code: Int): XProcException = {
    extensionError((code, 1), List())
  }

  // ====================================================================================

  def mapPipelineException(ex: Exception): Exception = {
    ex match {
      case jex: JafplException =>
        jex.code match {
          case JafplException.DUP_OPTION_NAME => XProcException.xsDupOptionName(jex.location, jex.details.head.asInstanceOf[String])
          // Port missing must be a cardinality error
          case JafplException.INPUT_PORT_MISSING => XProcException.xdInputSequenceNotAllowed(jex.details.head.asInstanceOf[String], jex.location)
          case JafplException.INPUT_CARDINALITY_ERROR => XProcException.xdInputSequenceNotAllowed(jex.details.head.asInstanceOf[String], jex.location)
          case JafplException.OUTPUT_CARDINALITY_ERROR => XProcException.xdOutputSequenceNotAllowed(jex.details.head.asInstanceOf[String], jex.location)
          case _ => jex
        }
      case _ => ex
    }
  }
}

class XProcException(val code: QName, val variant: Int, val message: Option[String], val location: Option[Location], val details: List[Any]) extends RuntimeException with JafplExceptionCode {
  private val _underlyingCauses = ListBuffer.empty[Exception]
  private var _errors = Option.empty[XdmNode]

  def this(code: QName) = {
    this(code, 1, None, None, List.empty[String])
  }

  def this(code: QName, message: String) = {
    this(code, 1, Some(message), None, List.empty[String])
  }

  def this(code: QName, message: String, location: Location) = {
    this(code, 1, Some(message), Some(location), List.empty[String])
  }

  def this(code: QName, message: String, context: StaticContext) = {
    this(code, 1, Some(message), context.location, List.empty[String])
  }

  def this(code: QName, context: StaticContext) = {
    this(code, 1, None, context.location, List.empty[String])
  }

  def withLocation(location: Location): XProcException = {
    new XProcException(code, variant, message, Some(location), details)
  }

  override def jafplExceptionCode: Any = code

  def underlyingCauses: List[Exception] = _underlyingCauses.toList
  def underlyingCauses_=(causes: List[Exception]): Unit = {
    _underlyingCauses ++= causes
  }

  def errors: Option[XdmNode] = _errors
  def errors_=(errors: XdmNode): Unit = {
    _errors = Some(errors)
  }

  override def getMessage: String = {
    message.getOrElse("")
  }

  override def toString: String = {
    var msg = "ERROR " + code

    if (location.isDefined) {
      msg += " " + location.get
    }

    if (message.isDefined) {
      msg += " " + message.get
    }

    msg
  }
}
