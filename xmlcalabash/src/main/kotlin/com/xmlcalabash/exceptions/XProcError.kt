package com.xmlcalabash.exceptions

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.NsErr
import net.sf.saxon.s9api.*
import java.net.URI
import java.util.*

open class XProcError protected constructor(val code: QName, val variant: Int, errorLocation: Location, val inputLocation: Location, vararg val details: Any) {
    companion object {
        val DEBUGGER_ABORT = 9997

        private fun staticError(code: Int): QName {
            return NsErr.xs(code)
        }

        private fun dynamicError(code: Int): QName {
            return NsErr.xd(code)
        }

        private fun stepError(code: Int): QName {
            return NsErr.xc(code)
        }

        private fun internalError(code: Int): QName {
            return NsErr.xi(code)
        }

        private fun xstepError(code: Int): QName {
            return NsErr.xxc(code)
        }

        private fun static(code: Pair<Int, Int>, vararg details: Any): XProcError {
            val ecode = staticError(code.first)
            return XProcError(ecode, code.second, Location.NULL, Location.NULL, *details)
        }

        fun static(code: Int, vararg details: Any): XProcError = static(Pair(code, 1), *details)

        private fun dynamic(code: Pair<Int,Int>, vararg details: Any): XProcError {
            val ecode = dynamicError(code.first)
            return XProcError(ecode, code.second, Location.NULL, Location.NULL, *details)
        }

        fun dynamic(code: Int, vararg details: Any): XProcError = dynamic(Pair(code, 1), *details)

        private fun step(code: Pair<Int,Int>, vararg details: Any): XProcError {
            val ecode = stepError(code.first)
            return XProcError(ecode, code.second, Location.NULL, Location.NULL, *details)
        }

        fun step(code: Int, vararg details: Any): XProcError = step(Pair(code, 1), *details)

        private fun internal(code: Pair<Int,Int>, vararg details: Any): XProcError {
            val ecode = internalError(code.first)
            return XProcError(ecode, code.second, Location.NULL, Location.NULL, *details)
        }

        fun internal(code: Int, vararg details: Any): XProcError = internal(Pair(code, 1), *details)

        private fun xstep(code: Pair<Int,Int>, vararg details: Any): XProcError {
            val ecode = xstepError(code.first)
            return XProcError(ecode, code.second, Location.NULL, Location.NULL, *details)
        }

        fun xstep(code: Int, vararg details: Any): XProcError = xstep(Pair(code, 1), *details)

        // ====================================================================================

        fun xsLoop(name: String) = static(1, name)
        fun xsDuplicateStepName(name: String) = static(2, name)
        fun xsNotConnected(port: String) = static(3, port)
        fun xsDuplicateOption(name: QName) = static(4, name)
        fun xsNoOutputConnection(name: String) = static(6, name)
        fun xsAttributeForbidden(name: QName) = static(8, name)
        fun xsDuplicatePortName(name: String) = static(11, name)
        fun xsMultiplePrimaryOutputPorts(name: String) = static(14, name)
        fun xsNoSteps() = static(15)
        fun xsRequiredAndDefaulted(name: QName) = static(17, name)
        fun xsMissingRequiredOption(name: QName) = static(18, name)
        fun xsPortNotReadable(step: String) = static(Pair(22, 1), step)
        fun xsPortNotReadable(step: String, port: String) = static(Pair(22, 2), step, port)
        fun xsOutputPortNotReadable(step: String, port: String) = static(Pair(22, 3), step, port)
        fun xsStepTypeInNoNamespace(type: QName) = static(Pair(25,1), type)
        fun xsStepTypeNotAllowed(type: QName) = static(Pair(25,2), type)
        fun xsOptionInXProcNamespace(name: QName) = static(28, name)
        fun xsOutputConnectionForbidden(port: String) = static(29, port)
        fun xsMultiplePrimaryInputPorts(name: String) = static(30, name)
        fun xsNoSuchOption(name: QName) = static(31, name)
        fun xsNoConnection(name: String) = static(32, name)
        fun xsDuplicateStepType(name: QName) = static(36, name)
        fun xsTextNotAllowed(text: String) = static(37, text)
        fun xsMissingRequiredAttribute(name: QName) = static(38, name)
        fun xsPortNameNotAllowed() = static(43)
        fun xsMissingStepDeclaration(name: QName) = static(44, name)
        fun xsNotImportable(uri: URI) = static(52, uri)
        fun xsInvalidExcludePrefix() = static(Pair(57, 1))
        fun xsInvalidExcludePrefix(prefix: String) = static(Pair(57, 2), prefix)
        fun xsNoDefaultNamespace() = static(58)
        fun xsNotAPipeline(name: QName) = static(59, name)
        fun xsUnsupportedVersion(ver: String) = static(60, ver)
        fun xsMissingVersion() = static(62)
        fun xsVersionMustBeDecimal(version: String) = static(63, version)
        fun xsMultipleCatchWithoutCodes() = static(Pair(64, 1))
        fun xsCatchWithoutCodesNotLast() = static(Pair(64, 2))
        fun xsCatchWithDuplicateCode(code: QName) = static(Pair(64, 3), code)
        fun xsNoPrimaryInput() = static(65)
        fun xsInvalidAVT(message: String) = static(66, message)
        fun xsNoStepPortNotReadable() = static(67)
        fun xsNoPortPortNotReadable() = static(68)
        fun xsUnsupportedEncoding(encoding: String) = static(69, encoding)
        fun xsDuplicateStaticOption(name: QName) = static(71, name)
        fun xsFinallyWithConflictingOutputs(name: String) = static(72, name)
        fun xsDependsNotAStep(name: String) = static(Pair(73, 1), name)
        fun xsDependsNotAllowed(name: String) = static(Pair(73, 2), name)
        fun xsWhenOrOtherwiseRequired() = static(74)
        fun xsTryWithoutSubpipeline() = static(Pair(75, 1))
        fun xsTryWithoutCatchOrFinally() = static(Pair(75, 2))
        fun xsTryWithMoreThanOneFinally() = static(Pair(75, 3))
        fun xsValueDoesNotSatisfyType(value: String, type: String) = static(77, value, type)
        fun xsInvalidImplicitInlineSiblings() = static(79)
        fun xsDuplicateWithOption(name: QName) = static(80, name)
        fun xsHrefAndChildren() = static(81)
        fun xsPipeAndChildren() = static(82)
        fun xsCatchCodesNotEQName(code: String) = static(83, code)
        fun xsHrefAndPipe() = static(85)
        fun xsDuplicatePortDeclaration(port: String) = static(86, port)
        fun xsUnboundPrefix(name: String) = static(87, name)
        fun xsShadowStaticOption(name: QName) = static(88, name)
        fun xsEmptyNotAllowed() = static(89)
        fun xsInvalidPipeAttribute(token: String) = static(90, token)
        fun xsVariableShadowsStaticOption(name: QName) = static(91, name)
        fun xsWithOptionForStatic(name: QName) = static(92, name)
        fun xsRequiredAndStatic(name: QName) = static(95, name)
        fun xsInvalidSequenceType(type: String) = static(96, type)
        fun xsAttributeNotAllowed(name: QName) = static(97, name)
        fun xsInvalidElement(name: QName) = static(Pair(100,1), name)
        fun xsInvalidImplicitInline(name: QName) = static(Pair(100,2), name)
        fun xsInvalidAttribute(name: QName) = static(Pair(100, 3), name)
        fun xsInvalidValues(value: String) = static(101, value)
        fun xsDifferentPrimaryOutputs() = static(102)
        fun xsUnknownFunctionMediaType(type: String) = static(104, type)
        fun xsImportFunctionsUnloadable(uri: URI) = static(106, uri)
        fun xsXPathStaticError(msg: String) = static(Pair(107,1), msg)
        fun xsXPathStaticError(name: QName) = static(Pair(107,2), name)
        fun xsPrimaryOutputRequiredOnIf() = static(108)
        fun xsLibraryOptionsMustBeStatic(name: QName) = static(109, name)
        fun xsInvalidContentType(value: String) = static(111, value)
        fun xsPrimaryOutputOnFinally(port: String) = static(112, port)
        fun xsInvalidExpandText(value: String) = static(113, value)
        fun xsNoSuchPort(port: String) = static(114, port)
        fun xsUseWhenDeadlock(cycle: String) = static(115, cycle)

        fun xdSequenceForbidden() = dynamic(Pair(1,1))
        fun xdEmptySequenceForbidden() = dynamic(Pair(1,2))
        fun xdInputSequenceForbidden(port: String) = dynamic(Pair(6, 1), port)
        fun xdInputRequiredOnPort(port: String) = dynamic(Pair(6, 2), port)
        fun xdOutputSequenceForbidden(port: String) = dynamic(7, port)
        fun xdViewportOnAttribute(expr: String) = dynamic(10, expr)
        fun xdDoesNotExist(path: String, message: String) = dynamic(Pair(11, 1), path, message)
        fun xdIsNotReadable(path: String, message: String) = dynamic(Pair(11, 2), path, message)
        fun xdIsNotWriteable(path: String, message: String) = dynamic(Pair(11, 3), path, message)
        fun xdInvalidQName(name: String) = dynamic(Pair(15, 1), name)
        fun xdNoBindingInScope(message: String) = dynamic(Pair(15, 2), message)
        fun xdInvalidSelection(name: QName) = dynamic(Pair(16, 1), name)
        fun xdInvalidFunctionSelection() = dynamic(Pair(16, 2))
        fun xdValueNotAllowed(value: XdmValue, allowed: List<XdmAtomicValue>) = dynamic(19, value, allowed)
        fun xdInvalidSerializationProperty() = dynamic(Pair(20,1))
        fun xdPsviUnsupported() = dynamic(22)
        fun xdNotDtdValid(msg: String) = dynamic(23, msg)
        fun xdValueDoesNotSatisfyType(value: String, type: String) = dynamic(28, value, type)
        fun xdStepFailed(message: String) = dynamic(30, message)
        fun xdBadType(value: String, type: String) = dynamic(Pair(36,1), value, type)
        fun xdBadType(value: String, type: ItemType) = dynamic(Pair(36,2), value, type)
        fun xdBadType(name: QName, value: String, type: String) = dynamic(Pair(36,3), name, value, type)
        fun xdBadType(message: String) = dynamic(Pair(36,4), message)
        fun xdBadInputContentType(port: String, type: String) = dynamic(38, port, type)
        fun xdUnsupportedCharset(charset: String) = dynamic(39, charset)
        fun xdBadBase64Input() = dynamic(40)
        fun xdBadOutputContentType(port: String, type: String) = dynamic(42, port, type)
        fun xdNotWellFormed() = dynamic(49)
        fun xdValueTemplateError(message: String) = dynamic(50, message)
        fun xdInvalidAvtResult(result: String) = dynamic(51, result)
        fun xdStepTimeout(timeout: String) = dynamic(53, timeout)
        fun xdEncodingWithXmlOrHtml(encoding: String) = dynamic(54, encoding)
        fun xdEncodingRequired(charset: String) = dynamic(55, charset)
        fun xdMarkupForbiddenWithEncoding(encoding: String) = dynamic(56, encoding)
        fun xdNotWellFormedJson(json: String) = dynamic(57, json)
        fun xdDuplicateKey(key: String) = dynamic(58, key)
        fun xdInvalidParameter(message: String) = dynamic(59, message)
        fun xdUnsupportedDocumentCharset(charset: String) = dynamic(60, charset)
        fun xdInvalidDocumentPropertyQName(value: String) = dynamic(61, value)
        fun xdContentTypesDiffer(contentType: String, propType: String) = dynamic(62, contentType, propType)
        fun xdMarkupForbidden(contentType: String) = dynamic(63, contentType)
        fun xdInvalidUri(uri: String) = dynamic(Pair(64, 1), uri)
        fun xdInvalidRelativeTo(uri: String) = dynamic(Pair(64, 2), uri)
        fun xdInlineContextSequence() = dynamic(65)
        fun xdInvalidPrefix(name: String, prefix: String) = dynamic(69, name, prefix)
        fun xdInvalidSerialization() = dynamic(Pair(70, 1))
        fun xdInvalidSerialization(value: String) = dynamic(Pair(70, 2), value)
        fun xdViewportNotXml() = dynamic(72)
        fun xdViewportResultNotXml() = dynamic(73)
        fun xdUrifyFailed(filepath: String, basedir: String) = dynamic(74, filepath, basedir)
        fun xdUrifyDifferentDrives(filepath: String, basedir: String) = dynamic(75, filepath, basedir)
        fun xdUrifyMixedDrivesAndAuthorities(filepath: String, basedir: String) = dynamic(76, filepath, basedir)
        fun xdUrifyDifferentSchemes(filepath: String, basedir: String) = dynamic(77, filepath, basedir)
        fun xdInvalidContentType(value: String) = dynamic(79, value)
        fun xdUrifyNonhierarchicalBase(filepath: String, basedir: String) = dynamic(80, filepath, basedir)

        fun xdTvtCannotSerializeAttributes(name: String) = dynamic(84, name)

        fun xcInvalidContentType(contentType: String) = step(1, contentType)
        fun xcHttpBadAuth(message: String) = step(3, message)
        fun xcXsltParameterNot20Compatible(name: QName, type: String) = step(7, name, type)
        fun xcXsltNoMode(mode: QName, message: String) = step(8, mode, message)
        fun xcXQueryVersionNotAvailable(version: String) = step(9, version)
        fun xcXmlSchemaVersionNotAvailable(version: String) = step(11, version)
        fun xcBadRenamePI() = step(13)
        fun xcNotADirectory(path: String) = step(17, path)
        fun xcComparisonFailed() = step(19)
        fun xcInvalidSelection(pattern: String) = step(Pair(23,1), pattern)
        fun xcInvalidSelection(pattern: String, nodeType: String) = step(Pair(23,2), pattern, nodeType)
        fun xcInvalidSelection(count: Int) = step(Pair(23,3), count)
        fun xcBadPosition(pattern: String, position: String) = step(24, pattern, position)
        fun xcBadTextPosition(pattern: String, position: String) = step(25, pattern, position)
        fun xcXIncludeError(message: String) = step(29, message)
        fun xcHttpCannotParseAs(contentType: MediaType) = step(30, contentType)
        fun xcOsExecMultipleInputs() = step(32)
        fun xcOsExecFailed() = step(33)
        fun xcOsExecBadCwd(cwd: String) = step(34, cwd)
        fun xcBadCrcVersion(version: String) = step(Pair(36,1), version)
        fun xcBadMdVersion(version: String) = step(Pair(36,2), version)
        fun xcBadShaVersion(version: String) = step(Pair(36,3), version)
        fun xcHashFailed(message: String) = step(Pair(36,4), message)
        fun xcBadHashAlgorithm(message: String) = step(Pair(36,5), message)
        fun xcMissingHmacKey() = step(Pair(36,6))
        fun xcHashBlake3ConflictingParameters() = step(Pair(36,7))
        fun xcHashBlake3IncompleteParameters() = step(Pair(36,8))
        fun xcHashBlake3Failed() = step(Pair(36,9))
        fun xcVersionNotAvailable(version: String) = step(38, version)
        fun xcInvalidUri(uri: URI) = step(Pair(50,1), uri)
        fun xcCannotCopy(source: URI, target: URI) = step(Pair(50,2), source, target)
        fun xcInvalidEncoding(encoding: String) = step(52, encoding)
        fun xcNotSchemaValidNVDL(xvrl: XProcDocument) = step(53, xvrl)
        fun xcNotSchemaValidSchematron(report: XProcDocument) = step(Pair(54, 1), report)
        fun xcNotSchemaValidSchematron(report: XProcDocument, uri: URI) = step(Pair(54, 2), report, uri)
        fun xcXsltNoTemplate(template: QName) = step(56, template)
        fun xcAllAndRelative() = step(58)
        fun xcCannotAddNamespaces(attName: QName) = step(Pair(59, 1), attName)
        fun xcCannotSetNamespaces() = step(Pair(59,2))
        fun xcUnsupportedUuidVersion(version: Int) = step(60, version)
        fun xcOsExecBadSeparator(sep: String) = step(63, sep)
        fun xcOsExecFailed(rc: Int) = step(64, rc)
        fun xcCannotSetContentType() = step(69)
        fun xcInvalidCharset(charset: String) = step(71, charset)
        fun xcNotBase64(message: String) = step(72, message)
        fun xcContentTypeRequired() = step(73)
        fun xcDifferentContentTypes(contentType: MediaType, dataContentType: MediaType) = step(74, contentType, dataContentType)
        fun xcComparisonMethodNotSupported(method: QName) = step(76, method)
        fun xcComparisonNotPossible(sourceType: MediaType, altType: MediaType) = step(77, sourceType, altType)
        fun xcFailOnTimeout(timeout: Int) = step(78, timeout)
        fun xcInvalidParameter(name: QName, value: String) = step(79, name, value)
        fun xcInvalidNumberOfArchives(number: Int) = step(80, number)

        fun xcArchiveFormatIncorrect(format: QName) = step(81, format)

        fun xcNoArchiveSourceUri() = step(Pair(84, 1))
        fun xcDuplicateArchiveSourceUri(uri: URI) = step(Pair(84, 2), uri)
        fun xcDuplicateArchiveSourceUri(name: String) = step(Pair(84, 3), name)

        fun xcInvalidArchiveFormat(format: QName) = step(Pair(85,1), format)
        fun xcCannotCreateArjArchives() = step(Pair(85, 2))
        fun xcUnrecognizedArchiveFormat(ctype: MediaType) = step(Pair(85, 3), ctype)

        fun xcUnsupportedScheme(scheme: String) = step(90, scheme)
        fun xcAttributeNameCollision(name: String) = step(92, name)
        fun xcXsltCompileError(message: String, exception: Exception) = step(93, message, exception)
        fun xcXsltInputNot20Compatible() = step(Pair(94,1))
        fun xcXsltInputNot20Compatible(media: MediaType) = step(Pair(94,2), media)
        fun xcXsltRuntimeError(message: String) = step(95, message)
        fun xcXsltUserTermination(message: String) = step(96, message)
        fun xcInvalidManifest() = step(Pair(100, 1))
        fun xcInvalidManifest(name: QName) = step(Pair(100, 2), name)
        fun xcInvalidManifestEntry(name: QName) = step(Pair(100, 3), name)
        fun xcInvalidManifestEntryName() = step(Pair(100, 4))
        fun xcInvalidManifestEntryHref() = step(Pair(100, 5))
        fun xcXQueryInputNot30Compatible(contentType: MediaType) = step(101, contentType)
        fun xcXQueryInvalidParameterType(name: QName, type: String) = step(102, name, type)
        fun xcXQueryCompileError(message: String, exception: Exception) = step(103, message, exception)
        fun xcXQueryEvalError(msg: String) = step(104, msg)
        fun xcDuplicateKeyInJsonMerge(key: XdmAtomicValue) = step(106, key)
        fun xcUnsupportedForJsonMerge() = step(107)
        fun xcNoNamespaceBindingForPrefix(prefix: String) = step(108, prefix)
        fun xcAttributeNameCollision(name: QName) = step(109, name)
        fun xcInvalidKeyForJsonMerge() = step(110)
        fun xcUnsupportedForJoin() = step(111)
        fun xcMultipleManifests() = step(112)
        fun xcDeleteFailed(path: String) = step(113, path)
        fun xcMkdirFailed(path: String) = step(114, path)
        fun xcAttemptToOverwrite(path: String) = step(115, path)
        fun xcTemporaryFileCreateFailed() = step(116)
        fun xcUnsupportedReportFormat(format: String) = step(117, format)
        fun xcInvalidFlatten(flatten: String) = step(119, flatten)
        fun xcNoUnarchiveBaseUri() = step(120)
        fun xcHttpInvalidAuth(name: String, value: String) = step(123, name, value)
        fun xcHttpInvalidParameter(name: String, value: String) = step(Pair(124, 1), name, value)
        fun xcHttpInvalidParameterType(name: String, value: String) = step(Pair(124, 2), name, value)
        fun xcHttpMultipartForbidden(href: URI) = step(125, href)
        fun xcHttpAssertionFailed(report: XdmNode) = step(126, report)
        fun xcHttpDuplicateHeader(name: String) = step(127, name)
        fun xcHttpUnsupportedScheme(scheme: String) = step(128, scheme)
        fun xcHttpUnsupportedHttpVersion(version: String) = step(129, version)
        fun xcUnsupportedTransferEncoding(encoding: String) = step(131, encoding)
        fun xcMultipartRequired(contentType: String) = step(133, contentType)
        fun xcUnsupportedFileInfoScheme(scheme: String) = step(134, scheme)
        fun xcUnsupportedFileTouchScheme(scheme: String) = step(136, scheme)
        fun xcUnsupportedFileCreateTempfileScheme(scheme: String) = step(138, scheme)
        fun xcUnsupportedFileMkdirScheme(scheme: String) = step(140, scheme)
        fun xcUnsupportedFileDeleteScheme(scheme: String) = step(142, scheme)
        fun xcUnsupportedFileCopyScheme(scheme: String) = step(144, scheme)
        fun xcBadOverrideContentTypesType() = step(Pair(146, 1))
        fun xcBadOverrideContentTypesMemberType(index: Int) = step(Pair(146, 2), index)
        fun xcBadOverrideContentTypesMemberTypeLength(length: Int) = step(Pair(146, 3), length)
        fun xcInvalidRegex(pattern: String) = step(147, pattern)
        fun xcUnsupportedFileMoveScheme(scheme: String) = step(148, scheme)
        fun xcNotSchematronSchema(name: QName) = step(151, name)
        fun xcXmlSchemaInvalidSchema() = step(Pair(152, 1))
        fun xcXmlSchemaInvalidSchema(uri: URI) = step(Pair(152, 2), uri)
        fun xcNotRelaxNG(message: String) = step(Pair(153,1), message)
        fun xcNotRelaxNG(uri: URI, message: String) = step(Pair(153,2), uri, message)
        fun xcNotNvdl(message: String) = step(154, message)
        fun xcNotSchemaValidRelaxNG(message: String) = step(Pair(155,1), message)
        fun xcNotSchemaValidRelaxNG(uri: URI, message: String) = step(Pair(155,2), uri, message)
        fun xcNotSchemaValidRelaxNG(xvrl: XProcDocument) = step(Pair(155,3), xvrl)
        fun xcNotSchemaValidXmlSchema(message: String) = step(Pair(156, 1), message)
        fun xcNotSchemaValidXmlSchema(uri: URI, message: String) = step(Pair(156, 2), uri, message)
        fun xcNotSchemaValidXmlSchema(xvrl: XProcDocument) = step(Pair(156, 3), xvrl)
        fun xcCopyDirectoryToFile(source: URI, target: URI) = step(157, source, target)
        fun xcMoveDirectoryToFile(source: URI, target: URI) = step(158, source, target)
        fun xcJsonSchemaNotSupported() = step(Pair(163, 1))
        fun xcJsonSchemaNotSupported(uri: URI) = step(Pair(163, 2), uri)
        fun xcJsonSchemaInvalid() = step(Pair(164, 1))
        fun xcJsonSchemaInvalid(uri: URI) = step(Pair(164, 2), uri)
        fun xcNotSchemaValidJson() = step(Pair(165, 1))
        fun xcNotSchemaValidJson(uri: URI) = step(Pair(165,2), uri)
        fun xcNotSchemaValidJson(xvrl: XProcDocument) = step(Pair(165,3), xvrl)
        fun xcNotAPipeline() = step(200)
        fun xcCannotUncompress(contentType: MediaType) = step(Pair(201,1), contentType)
        fun xcUnsupportedCompressionFormat(format: QName) = step(Pair(202, 1), format)
        fun xcCannotUncompress() = step(Pair(202,2))
        fun xcHttpInvalidBoundary(boundary: String) = step(203, boundary)
        fun xcUnsupportedContentType(contentType: MediaType) = step(204, contentType)
        fun xcRunInputPrimaryMismatch(primary: String) = step(Pair(206, 1), primary)
        fun xcRunInputPrimaryMismatch(primary: String, runPrimary: String) = step(Pair(206, 2), primary, runPrimary)
        fun xcRunInputPrimaryUndeclared(runPrimary: String) = step(Pair(206, 3), runPrimary)
        fun xcRunOutputPrimaryMismatch(primary: String) = step(Pair(207, 1), primary)
        fun xcRunOutputPrimaryMismatch(primary: String, runPrimary: String) = step(Pair(207, 2), primary, runPrimary)
        fun xcRunOutputPrimaryUndeclared(runPrimary: String) = step(Pair(207, 3), runPrimary)
        fun xcDtdValidationFailed(xvrl: XProcDocument) = step(210, xvrl)
        fun xcAtMostOneGrammar() = step(211)
        fun xcInvalidIxmlGrammar() = step(212)

        fun xiCastUnsupported(message: String) = xstep(1, message)
        fun xiCastInputIncorrect(message: String) = xstep(2, message)
        fun xcxResourceWithoutUri() = xstep(3)
        fun xcxResourceWithDuplicateUri(uri: URI) = xstep(4, uri)
        fun xcxResourceWithoutValue(uri: URI) = xstep(5, uri)
        fun xcxCannotModifyStableCollection(uri: String) = xstep(6, uri)
        fun xcxOffsetOutOfRange(offset: Int) = xstep(7, offset)
        fun xcxInvalidHexColor(color: String) = xstep(8, color)
        fun xcxNoStyleDefinitions() = xstep(9)
        fun xcxNonterminalNotFound(nonterminal: String) = xstep(10, nonterminal)
        fun xcxInvalidWidth(width: Int) = xstep(11, width)

        fun xiNoSuchOutputPort(port: String)= internal(1, port)
        fun xiImpossibleNodeType(type: XdmNodeKind) = internal(2, type)
        fun xiThreadInterrupted() = internal(3)
        fun xiCannotCastTo(to: MediaType) = internal(4, to)
        fun xiCannotLoadResource(path: String) = internal(5, path)
        fun xiConfigurationInvalid(file: String) = internal(Pair(17, 1), file)
        fun xiConfigurationInvalid(file: String, message: String) = internal(Pair(17, 2), file, message)
        fun xiUnreadableFile(filename: String) = internal(21, filename)
        fun xiUnrecognizedConfigurationProperty(name: QName) = internal(22, name)
        fun xiUnrecognizedConfigurationAttribute(elemName: QName, attrName: QName) = internal(25, elemName, attrName)
        fun xiMissingConfigurationAttribute(elemName: QName, attrName: QName) = internal(Pair(26, 1), elemName, attrName)
        fun xiMissingConfigurationAttributes(elemName: QName, message: String) = internal(Pair(26, 2), elemName, message)
        fun xiForbiddenConfigurationAttributes(formatter: URI) = internal(Pair(26, 3), formatter)
        fun xiUnrecognizedConfigurationValue(elemName: QName, attrName: QName, value: String) = internal(27, elemName, attrName, value)
        fun xiUnrecognizedSaxonConfigurationProperty(key: String) = internal(28, key)
        fun xiInvalidSaxonConfigurationProperty(key: String, value: String) = internal(29, key, value)
        fun xiBaseUriRequiredToCache() = internal(30)
        fun xiCannotFindGraphviz(value: String) = internal(31, value)
        fun xiCannotExecuteGraphviz(value: String) = internal(32, value)
        fun xiUnwritableOutputFile(filename: String) = internal(33, filename)
        fun xiNotASpecialType(type: String) = internal(36, type)
        fun xiDocumentInCache(href: URI) = internal(37, href)
        fun xiDocumentNotInCache(href: URI) = internal(38, href)
        fun xiNoPipelineInLibrary(href: String) = internal(Pair(39, 1), href)
        fun xiNoPipelineInLibrary(name: String, href: String) = internal(Pair(39, 2), name, href)
        fun xiInitializerError(message: String) = internal(Pair(40, 1), message)
        fun xiAssertionFailed(message: String) = internal(Pair(41,1), message)
        fun xiAssertionFailed(code: String, message: String) = internal(Pair(41,2), message)
        fun xiPipeinfoMustBeEmpty() = internal(43)
        fun xiPipeinfoMustExist(message: String) = internal(44, message)
        fun xiPipeinfoMustBePipeinfo(message: String) = internal(Pair(45, 1), message)
        fun xiPipeinfoMustBePipeinfo(root: QName) = internal(Pair(45, 2), root)
        fun xiXPathVersionNotSupported(version: String) = internal(46, version)
        fun xiTooManySources(count: Int) = internal(47, count)

        fun xiCliInvalidValue(option: String, value: String) = internal(200, option, value)
        fun xiCliValueRequired(option: String) = internal(202, option)
        fun xiCliUnrecognizedOption(name: String) = internal(203, name)
        fun xiCliMoreThanOnePipeline(first: String, second: String) = internal(204, first, second)
        fun xiCliMalformedOption(type: String, opt: String) = internal(205, type, opt)
        fun xiCliDuplicateOutputFile(filename: String) = internal(206, filename)

        fun xiTooLateForStaticOptions(name: QName) = internal(213, name)
        fun xiCliDuplicateNamespace(prefix: String) = internal(214, prefix)
        fun xiMergeDuplicatesError(name: String) = internal(215, name)

        fun xiXvrlInvalidValue(name: QName, value: String) = internal(300, name, value)
        fun xiXvrlIllegalMessageName(name: QName) = internal(301, name)
        fun xiXvrlIllegalMessageAttribute(name: QName) = internal(302, name)
        fun xiXvrlIllegalCommonAttribute(name: QName) = internal(303, name)
        fun xiXvrlInvalidSeverity(severity: String) = internal(304, severity)
        fun xiXvrlInvalidMessage(message: String) = internal(305, message)
        fun xiXvrlInvalidValid(valid: String) = internal(306, valid)
        fun xiXvrlInvalidWorst(worst: String) = internal(307, worst)
        fun xiXvrlNullCode() = internal(308)

        fun xiAtMostOneStdout() = internal(320)

        fun xiAbortDebugger() = internal(DEBUGGER_ABORT) // 9997
        fun xiImpossible(message: String) = internal(9998, message)
        fun xiNotImplemented(message: String) = internal(9999, message)
    }

    var _location = errorLocation
    val location: Location
        get() = _location

    var _throwable: Throwable? = null
    val throwable: Throwable?
        get() = _throwable
    val stackTrace = Stack<ErrorStackFrame>()

    private constructor(error: XProcError, location: Location, inputLocation: Location): this(error.code, error.variant, location, inputLocation, *error.details) {
        _throwable = error.throwable
        stackTrace.addAll(error.stackTrace)
    }

    fun asStatic(): XProcError {
        if (code == NsErr.xd(36)) {
            val detail0 = details[0].toString()
            val detail1 = details[1].toString()
            return xsValueDoesNotSatisfyType(detail0, detail1)
        }

        if (code == NsErr.xd(69)) {
            return xsUnboundPrefix(details[0].toString())
        }

        // Don't do this one, xd0079 is bad content type, xs0111 is bad content type shortcut
        /*
        if (code == errXD0079) {
            val detail0 = details[0].toString()
            return xsInvalidContentType(detail0)
        }
         */

        return this
    }

    fun at(location: Location): XProcError {
        if (location != Location.NULL) {
            return XProcError(code, variant, location, inputLocation, *details)
        }
        return this
    }

    fun at(saxonLocation: net.sf.saxon.s9api.Location?): XProcError {
        if (saxonLocation != null && saxonLocation.systemId != null) {
            val xloc = Location(URI(saxonLocation.systemId), saxonLocation.lineNumber, saxonLocation.columnNumber)
            return XProcError(code, variant, location, xloc, *details)
        }
        return this
    }

    fun at(node: XdmNode): XProcError {
        // The document node doesn't have a useful location; get the location of the first element if we can
        val locNode = if (node.nodeKind == XdmNodeKind.DOCUMENT) {
            var seek: XdmNode = node
            for (child in node.axisIterator(Axis.CHILD)) {
                if (child.nodeKind == XdmNodeKind.ELEMENT) {
                    seek = child;
                    break;
                }
            }
            seek
        } else {
            node
        }

        return XProcError(this, location, Location(locNode))
    }

    fun at(doc: XProcDocument): XProcError {
        return XProcError(this, location, Location(doc.baseURI))
    }

    fun updateAt(type: QName, name: String): XProcError {
        // N.B. mutates the object, doesn't make a copy
        return updateAt(ErrorStackFrame(type, name))
    }

    fun updateAt(frame: ErrorStackFrame): XProcError {
        // N.B. mutates the object, doesn't make a copy
        stackTrace.push(frame)
        return this
    }

    fun updateAt(location: Location): XProcError {
        if (location != Location.NULL) {
            this._location = location
        }
        return this
    }

    fun with(newCode: QName): XProcError {
        return XProcError(newCode, 1, location, inputLocation, *details)
    }

    fun with(cause: Throwable): XProcError {
        _throwable = cause
        return this
    }

    fun exception(): XProcException {
        return XProcException(this)
    }

    fun exception(cause: Throwable): XProcException {
        _throwable = cause
        return XProcException(this, cause)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(code.toString())
        when (details.size) {
            0 -> Unit
            1 -> sb.append(": ").append(details[0])
            else -> {
                sb.append(": [")
                for (index in details.indices) {
                    if (index > 0) {
                        sb.append(", ")
                    }
                    sb.append(details[index])
                }
                sb.append("]")
            }
        }
        return sb.toString()
    }
}
