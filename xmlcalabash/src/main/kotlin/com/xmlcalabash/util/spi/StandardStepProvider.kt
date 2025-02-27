package com.xmlcalabash.util.spi

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.parameters.DocumentStepParameters
import com.xmlcalabash.runtime.parameters.EmptyStepParameters
import com.xmlcalabash.runtime.parameters.ExpressionStepParameters
import com.xmlcalabash.runtime.parameters.InlineStepParameters
import com.xmlcalabash.runtime.parameters.OptionStepParameters
import com.xmlcalabash.runtime.parameters.SelectStepParameters
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.runtime.parameters.UnimplementedStepParameters
import com.xmlcalabash.spi.AtomicStepManager
import com.xmlcalabash.spi.AtomicStepProvider
import com.xmlcalabash.steps.AddAttributeStep
import com.xmlcalabash.steps.AddXmlBaseStep
import com.xmlcalabash.steps.ArchiveManifestStep
import com.xmlcalabash.steps.ArchiveStep
import com.xmlcalabash.steps.CastContentTypeStep
import com.xmlcalabash.steps.CompareStep
import com.xmlcalabash.steps.CompressStep
import com.xmlcalabash.steps.CountStep
import com.xmlcalabash.steps.CssFormatterStep
import com.xmlcalabash.steps.DeleteStep
import com.xmlcalabash.steps.EncodeStep
import com.xmlcalabash.steps.ErrorStep
import com.xmlcalabash.steps.FilterStep
import com.xmlcalabash.steps.HashStep
import com.xmlcalabash.steps.HttpRequestStep
import com.xmlcalabash.steps.IdentityStep
import com.xmlcalabash.steps.InsertStep
import com.xmlcalabash.steps.InvisibleXmlStep
import com.xmlcalabash.steps.JsonJoinStep
import com.xmlcalabash.steps.JsonMergeStep
import com.xmlcalabash.steps.LabelElementsStep
import com.xmlcalabash.steps.LoadStep
import com.xmlcalabash.steps.MakeAbsoluteUrisStep
import com.xmlcalabash.steps.MarkdownToHtml
import com.xmlcalabash.steps.MessageStep
import com.xmlcalabash.steps.NamespaceDeleteStep
import com.xmlcalabash.steps.NamespaceRenameStep
import com.xmlcalabash.steps.PackStep
import com.xmlcalabash.steps.RenameStep
import com.xmlcalabash.steps.ReplaceStep
import com.xmlcalabash.steps.SetAttributesStep
import com.xmlcalabash.steps.SetPropertiesStep
import com.xmlcalabash.steps.SinkStep
import com.xmlcalabash.steps.SleepStep
import com.xmlcalabash.steps.SplitSequenceStep
import com.xmlcalabash.steps.StoreStep
import com.xmlcalabash.steps.StringReplaceStep
import com.xmlcalabash.steps.TextCountStep
import com.xmlcalabash.steps.TextHeadStep
import com.xmlcalabash.steps.TextJoinStep
import com.xmlcalabash.steps.TextReplaceStep
import com.xmlcalabash.steps.TextSortStep
import com.xmlcalabash.steps.TextTailStep
import com.xmlcalabash.steps.UnarchiveStep
import com.xmlcalabash.steps.UncompressStep
import com.xmlcalabash.steps.UnwrapStep
import com.xmlcalabash.steps.UuidStep
import com.xmlcalabash.steps.WrapSequenceStep
import com.xmlcalabash.steps.WrapStep
import com.xmlcalabash.steps.WwwFormUrlDecodeStep
import com.xmlcalabash.steps.WwwFormUrlEncodeStep
import com.xmlcalabash.steps.XIncludeStep
import com.xmlcalabash.steps.XQueryStep
import com.xmlcalabash.steps.XslFormatterStep
import com.xmlcalabash.steps.XsltStep
import com.xmlcalabash.steps.extension.CacheDocument
import com.xmlcalabash.steps.extension.UncacheDocument
import com.xmlcalabash.steps.file.CreateTempfileStep
import com.xmlcalabash.steps.file.DirectoryListStep
import com.xmlcalabash.steps.file.FileCopyStep
import com.xmlcalabash.steps.file.FileDeleteStep
import com.xmlcalabash.steps.file.FileInfoStep
import com.xmlcalabash.steps.file.FileMkdirStep
import com.xmlcalabash.steps.file.FileMoveStep
import com.xmlcalabash.steps.file.FileTouchStep
import com.xmlcalabash.steps.internal.DocumentStep
import com.xmlcalabash.steps.internal.EmptyStep
import com.xmlcalabash.steps.internal.ExpressionStep
import com.xmlcalabash.steps.internal.GuardStep
import com.xmlcalabash.steps.internal.InlineStep
import com.xmlcalabash.steps.internal.JoinerStep
import com.xmlcalabash.steps.internal.NullStep
import com.xmlcalabash.steps.internal.OptionExpressionStep
import com.xmlcalabash.steps.internal.SelectStep
import com.xmlcalabash.steps.internal.SplitterStep
import com.xmlcalabash.steps.internal.UnimplementedStep
import com.xmlcalabash.steps.os.OsExec
import com.xmlcalabash.steps.os.OsInfo
import com.xmlcalabash.steps.validation.ValidateWithDTD
import com.xmlcalabash.steps.validation.ValidateWithJsonSchema
import com.xmlcalabash.steps.validation.ValidateWithNVDL
import com.xmlcalabash.steps.validation.ValidateWithRelaxNG
import com.xmlcalabash.steps.validation.ValidateWithSchematron
import com.xmlcalabash.steps.validation.ValidateWithXmlSchema
import net.sf.saxon.s9api.QName

class StandardStepProvider: AtomicStepManager, AtomicStepProvider {
    val implMap = mapOf(
        NsP.addAttribute        to { _: StepParameters? -> AddAttributeStep() },
        NsP.addXmlBase          to { _: StepParameters? -> AddXmlBaseStep() },
        NsP.archive             to { _: StepParameters? -> ArchiveStep() },
        NsP.archiveManifest     to { _: StepParameters? -> ArchiveManifestStep() },
        NsP.castContentType     to { _: StepParameters? -> CastContentTypeStep() },
        NsP.compare             to { _: StepParameters? -> CompareStep() },
        NsP.compress            to { _: StepParameters? -> CompressStep() },
        NsP.count               to { _: StepParameters? -> CountStep() },
        NsP.cssFormatter        to { _: StepParameters? -> CssFormatterStep() },
        NsP.delete              to { _: StepParameters? -> DeleteStep() },
        NsP.directoryList       to { _: StepParameters? -> DirectoryListStep() },
        NsP.encode              to { _: StepParameters? -> EncodeStep() },
        NsP.error               to { _: StepParameters? -> ErrorStep() },
        NsP.fileCopy            to { _: StepParameters? -> FileCopyStep() },
        NsP.fileCreateTempfile  to { _: StepParameters? -> CreateTempfileStep() },
        NsP.fileDelete          to { _: StepParameters? -> FileDeleteStep() },
        NsP.fileInfo            to { _: StepParameters? -> FileInfoStep() },
        NsP.fileMkdir           to { _: StepParameters? -> FileMkdirStep() },
        NsP.fileMove            to { _: StepParameters? -> FileMoveStep() },
        NsP.fileTouch           to { _: StepParameters? -> FileTouchStep() },
        NsP.filter              to { _: StepParameters? -> FilterStep() },
        NsP.hash                to { _: StepParameters? -> HashStep() },
        NsP.httpRequest         to { _: StepParameters? -> HttpRequestStep() },
        NsP.identity            to { _: StepParameters? -> IdentityStep() },
        NsP.invisibleXml        to { _: StepParameters? -> InvisibleXmlStep() },
        NsP.insert              to { _: StepParameters? -> InsertStep() },
        NsP.ixml                to { _: StepParameters? -> InvisibleXmlStep() },
        NsP.jsonJoin            to { _: StepParameters? -> JsonJoinStep() },
        NsP.jsonMerge           to { _: StepParameters? -> JsonMergeStep() },
        NsP.labelElements       to { _: StepParameters? -> LabelElementsStep() },
        NsP.load                to { _: StepParameters? -> LoadStep() },
        NsP.makeAbsoluteUris    to { _: StepParameters? -> MakeAbsoluteUrisStep() },
        NsP.markdownToHtml      to { _: StepParameters? -> MarkdownToHtml() },
        NsP.message             to { _: StepParameters? -> MessageStep() },
        NsP.namespaceDelete     to { _: StepParameters? -> NamespaceDeleteStep() },
        NsP.namespaceRename     to { _: StepParameters? -> NamespaceRenameStep() },
        NsP.osExec              to { _: StepParameters? -> OsExec() },
        NsP.osInfo              to { _: StepParameters? -> OsInfo() },
        NsP.pack                to { _: StepParameters? -> PackStep() },
        NsP.rename              to { _: StepParameters? -> RenameStep() },
        NsP.replace             to { _: StepParameters? -> ReplaceStep() },
        NsP.setAttributes       to { _: StepParameters? -> SetAttributesStep() },
        NsP.setProperties       to { _: StepParameters? -> SetPropertiesStep() },
        NsP.sink                to { _: StepParameters? -> SinkStep() },
        NsP.sleep               to { _: StepParameters? -> SleepStep() },
        NsP.splitSequence       to { _: StepParameters? -> SplitSequenceStep() },
        NsP.store               to { _: StepParameters? -> StoreStep() },
        NsP.stringReplace       to { _: StepParameters? -> StringReplaceStep() },
        NsP.textCount           to { _: StepParameters? -> TextCountStep() },
        NsP.textHead            to { _: StepParameters? -> TextHeadStep() },
        NsP.textJoin            to { _: StepParameters? -> TextJoinStep() },
        NsP.textReplace         to { _: StepParameters? -> TextReplaceStep() },
        NsP.textSort            to { _: StepParameters? -> TextSortStep() },
        NsP.textTail            to { _: StepParameters? -> TextTailStep() },
        NsP.unarchive           to { _: StepParameters? -> UnarchiveStep() },
        NsP.uncompress          to { _: StepParameters? -> UncompressStep() },
        NsP.unwrap              to { _: StepParameters? -> UnwrapStep() },
        NsP.uuid                to { _: StepParameters? -> UuidStep() },
        NsP.wrap                to { _: StepParameters? -> WrapStep() },
        NsP.wrapSequence        to { _: StepParameters? -> WrapSequenceStep() },
        NsP.wwwFormUrldecode    to { _: StepParameters? -> WwwFormUrlDecodeStep() },
        NsP.wwwFormUrlencode    to { _: StepParameters? -> WwwFormUrlEncodeStep() },
        NsP.validateWithDTD         to { _: StepParameters? -> ValidateWithDTD() },
        NsP.validateWithNVDL        to { _: StepParameters? -> ValidateWithNVDL() },
        NsP.validateWithRelaxNG     to { _: StepParameters? -> ValidateWithRelaxNG() },
        NsP.validateWithSchematron  to { _: StepParameters? -> ValidateWithSchematron() },
        NsP.validateWithXmlSchema   to { _: StepParameters? -> ValidateWithXmlSchema() },
        NsP.validateWithJsonSchema  to { _: StepParameters? -> ValidateWithJsonSchema() },
        NsP.xinclude            to { _: StepParameters? -> XIncludeStep() },
        NsP.xquery              to { _: StepParameters? -> XQueryStep() },
        NsP.xslFormatter        to { _: StepParameters? -> XslFormatterStep() },
        NsP.xslt                to { _: StepParameters? -> XsltStep() },

        NsCx.cacheAddDocument   to { _: StepParameters? -> CacheDocument() },
        NsCx.cacheRemoveDocument to { _: StepParameters? -> UncacheDocument() },
        NsCx.guard              to { _: StepParameters? -> GuardStep() },
        NsCx.expression         to { p: StepParameters? -> ExpressionStep(p as ExpressionStepParameters) },
        NsCx.option             to { p: StepParameters? -> OptionExpressionStep(p as OptionStepParameters) },
        NsCx.inline             to { p: StepParameters? -> InlineStep(p as InlineStepParameters) },
        NsCx.document           to { p: StepParameters? -> DocumentStep(p as DocumentStepParameters) },
        NsCx.empty              to { p: StepParameters? -> EmptyStep(p as EmptyStepParameters) },
        NsCx.joiner             to { _: StepParameters? -> JoinerStep() },
        NsCx.sink               to { _: StepParameters? -> SinkStep() },
        NsCx.splitter           to { _: StepParameters? -> SplitterStep() },
        NsCx.select             to { p: StepParameters? -> SelectStep(p as SelectStepParameters) },
        NsCx.head               to { _: StepParameters? -> NullStep() },
        NsCx.foot               to { _: StepParameters? -> NullStep() },
        NsCx.unimplemented      to { p: StepParameters? -> UnimplementedStep(p as UnimplementedStepParameters) }
    )

    override fun create(): AtomicStepManager {
        return this
    }

    override fun stepTypes(): Set<QName> {
        return implMap.keys
    }

    override fun stepAvailable(stepType: QName): Boolean {
        return implMap[stepType] != null
    }

    override fun createStep(params: StepParameters): () -> XProcStep {
        val constructor = implMap[params.stepType]
            ?: throw XProcError.Companion.xiNotImplemented("createStep for ${params.stepType}").exception()
        return { -> constructor(params) }
    }
}