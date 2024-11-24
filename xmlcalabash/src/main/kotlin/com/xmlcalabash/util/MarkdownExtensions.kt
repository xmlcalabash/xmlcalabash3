package com.xmlcalabash.util

// This class is generated. Do not edit.

import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension
import com.vladsch.flexmark.ext.admonition.AdmonitionExtension
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.ext.aside.AsideExtension
import com.vladsch.flexmark.ext.attributes.AttributeImplicitName
import com.vladsch.flexmark.ext.attributes.AttributeValueQuotes
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.attributes.FencedCodeAddType
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.emoji.EmojiImageType
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType
import com.vladsch.flexmark.ext.enumerated.reference.EnumeratedReferenceExtension
import com.vladsch.flexmark.ext.escaped.character.EscapedCharacterExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.issues.GfmIssuesExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItemCase
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItemPlacement
import com.vladsch.flexmark.ext.gfm.users.GfmUsersExtension
import com.vladsch.flexmark.ext.gitlab.GitLabExtension
import com.vladsch.flexmark.ext.ins.InsExtension
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension
import com.vladsch.flexmark.ext.macros.MacrosExtension
import com.vladsch.flexmark.ext.media.tags.MediaTagsExtension
import com.vladsch.flexmark.ext.resizable.image.ResizableImageExtension
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.SimTocExtension
import com.vladsch.flexmark.ext.toc.SimTocGenerateOnFormat
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.ext.toc.internal.TocOptions
import com.vladsch.flexmark.ext.typographic.TypographicExtension
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension
import com.vladsch.flexmark.ext.xwiki.macros.MacroExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.ext.youtube.embedded.YouTubeLinkExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.KeepType
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.DataSet
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.data.NullableDataKey
import com.vladsch.flexmark.util.format.options.DefinitionMarker
import com.vladsch.flexmark.util.format.options.DiscretionaryText
import com.vladsch.flexmark.util.format.options.ElementPlacement
import com.vladsch.flexmark.util.format.options.ElementPlacementSort
import com.vladsch.flexmark.util.format.options.TableCaptionHandling

/** Generated class of options for p:markdown-to-html extensions.
 * <p>These are the extensions for com.vladsch.flexmark:flexmark-all:0.64.8.</p>
 */

class MarkdownExtensions() {
    private val parserExtensions = mutableSetOf<Parser.ParserExtension>()
    private val _options = MutableDataSet()
    private var once = true
    val options: MutableDataSet
        get() {
            if (once && parserExtensions.isNotEmpty()) {
                _options.set(Parser.EXTENSIONS, parserExtensions.toList())
            }
            once = false
            return _options
        }

    val extensions = mapOf(
        "abbreviation" to { AbbreviationExtension.create() },
        "admonition" to { AdmonitionExtension.create() },
        "anchorlink" to { AnchorLinkExtension.create() },
        "aside" to { AsideExtension.create() },
        "attributes" to { AttributesExtension.create() },
        "autolink" to { AutolinkExtension.create() },
        "definition" to { DefinitionExtension.create() },
        "emoji" to { EmojiExtension.create() },
        "enumeratedreference" to { EnumeratedReferenceExtension.create() },
        "escaped-character" to { EscapedCharacterExtension.create() },
        "footnotes" to { FootnoteExtension.create() },
        "gfm-issues" to { GfmIssuesExtension.create() },
        "gfm-strikethrough" to { StrikethroughExtension.create() },
        "gfm-strikethrough-subscript" to { SubscriptExtension.create() },
        "gfm-strikethrough-strikethrough-subscript" to { StrikethroughSubscriptExtension.create() },
        "gfm-tasklist" to { TaskListExtension.create() },
        "gfm-users" to { GfmUsersExtension.create() },
        "gitlab" to { GitLabExtension.create() },
        "ins" to { InsExtension.create() },
        "jekyll-front-matter" to { JekyllFrontMatterExtension.create() },
        "macros" to { MacrosExtension.create() },
        "media-tags" to { MediaTagsExtension.create() },
        "resizable-image" to { ResizableImageExtension.create() },
        "superscript" to { SuperscriptExtension.create() },
        "tables" to { TablesExtension.create() },
        "sim-toc" to { SimTocExtension.create() },
        "toc" to { TocExtension.create() },
        "typographic" to { TypographicExtension.create() },
        "wikilink" to { WikiLinkExtension.create() },
        "xwiki-macros" to { MacroExtension.create() },
        "yaml-front-matter" to { YamlFrontMatterExtension.create() },
        "youtube-embedded" to { YouTubeLinkExtension.create() },
    )

    val extensionOptions = mapOf(
        "abbreviation" to mapOf(
            "abbreviations-keep" to DataKeyEnumerationOption(mapOf(
                "last" to DataKeyKeepType(AbbreviationExtension.ABBREVIATIONS_KEEP, KeepType.LAST),
                "first" to DataKeyKeepType(AbbreviationExtension.ABBREVIATIONS_KEEP, KeepType.FIRST),
                "fail" to DataKeyKeepType(AbbreviationExtension.ABBREVIATIONS_KEEP, KeepType.FAIL),
                "locked" to DataKeyKeepType(AbbreviationExtension.ABBREVIATIONS_KEEP, KeepType.LOCKED)
            )),
            "use-links" to DataKeyBoolean(AbbreviationExtension.USE_LINKS),
            "abbreviations-placement" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyElementPlacement(AbbreviationExtension.ABBREVIATIONS_PLACEMENT, ElementPlacement.AS_IS),
                "document-top" to DataKeyElementPlacement(AbbreviationExtension.ABBREVIATIONS_PLACEMENT, ElementPlacement.DOCUMENT_TOP),
                "group-with-first" to DataKeyElementPlacement(AbbreviationExtension.ABBREVIATIONS_PLACEMENT, ElementPlacement.GROUP_WITH_FIRST),
                "group-with-last" to DataKeyElementPlacement(AbbreviationExtension.ABBREVIATIONS_PLACEMENT, ElementPlacement.GROUP_WITH_LAST),
                "document-bottom" to DataKeyElementPlacement(AbbreviationExtension.ABBREVIATIONS_PLACEMENT, ElementPlacement.DOCUMENT_BOTTOM)
            )),
            "abbreviations-sort" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyElementPlacementSort(AbbreviationExtension.ABBREVIATIONS_SORT, ElementPlacementSort.AS_IS),
                "sort" to DataKeyElementPlacementSort(AbbreviationExtension.ABBREVIATIONS_SORT, ElementPlacementSort.SORT),
                "sort-unused-last" to DataKeyElementPlacementSort(AbbreviationExtension.ABBREVIATIONS_SORT, ElementPlacementSort.SORT_UNUSED_LAST),
                "sort-delete-unused" to DataKeyElementPlacementSort(AbbreviationExtension.ABBREVIATIONS_SORT, ElementPlacementSort.SORT_DELETE_UNUSED),
                "delete-unused" to DataKeyElementPlacementSort(AbbreviationExtension.ABBREVIATIONS_SORT, ElementPlacementSort.DELETE_UNUSED)
            )),
            "make-merged-abbreviations-unique" to DataKeyBoolean(AbbreviationExtension.MAKE_MERGED_ABBREVIATIONS_UNIQUE),
        ),
        "admonition" to mapOf(
            "content-indent" to DataKeyInteger(AdmonitionExtension.CONTENT_INDENT),
            "allow-leading-space" to DataKeyBoolean(AdmonitionExtension.ALLOW_LEADING_SPACE),
            "interrupts-paragraph" to DataKeyBoolean(AdmonitionExtension.INTERRUPTS_PARAGRAPH),
            "interrupts-item-paragraph" to DataKeyBoolean(AdmonitionExtension.INTERRUPTS_ITEM_PARAGRAPH),
            "with-spaces-interrupts-item-paragraph" to DataKeyBoolean(AdmonitionExtension.WITH_SPACES_INTERRUPTS_ITEM_PARAGRAPH),
            "allow-lazy-continuation" to DataKeyBoolean(AdmonitionExtension.ALLOW_LAZY_CONTINUATION),
            "unresolved-qualifier" to DataKeyString(AdmonitionExtension.UNRESOLVED_QUALIFIER),
            "qualifier-type-map" to DataKeyMapStringString(AdmonitionExtension.QUALIFIER_TYPE_MAP),
            "qualifier-title-map" to DataKeyMapStringString(AdmonitionExtension.QUALIFIER_TITLE_MAP),
            "type-svg-map" to DataKeyMapStringString(AdmonitionExtension.TYPE_SVG_MAP),
        ),
        "anchorlink" to mapOf(
            "anchorlinks-wrap-text" to DataKeyBoolean(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT),
            "anchorlinks-text-prefix" to DataKeyString(AnchorLinkExtension.ANCHORLINKS_TEXT_PREFIX),
            "anchorlinks-text-suffix" to DataKeyString(AnchorLinkExtension.ANCHORLINKS_TEXT_SUFFIX),
            "anchorlinks-anchor-class" to DataKeyString(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS),
            "anchorlinks-set-name" to DataKeyBoolean(AnchorLinkExtension.ANCHORLINKS_SET_NAME),
            "anchorlinks-set-id" to DataKeyBoolean(AnchorLinkExtension.ANCHORLINKS_SET_ID),
            "anchorlinks-no-block-quote" to DataKeyBoolean(AnchorLinkExtension.ANCHORLINKS_NO_BLOCK_QUOTE),
        ),
        "aside" to mapOf(
            "extend-to-blank-line" to DataKeyBoolean(AsideExtension.EXTEND_TO_BLANK_LINE),
            "ignore-blank-line" to DataKeyBoolean(AsideExtension.IGNORE_BLANK_LINE),
            "allow-leading-space" to DataKeyBoolean(AsideExtension.ALLOW_LEADING_SPACE),
            "interrupts-paragraph" to DataKeyBoolean(AsideExtension.INTERRUPTS_PARAGRAPH),
            "interrupts-item-paragraph" to DataKeyBoolean(AsideExtension.INTERRUPTS_ITEM_PARAGRAPH),
            "with-lead-spaces-interrupts-item-paragraph" to DataKeyBoolean(AsideExtension.WITH_LEAD_SPACES_INTERRUPTS_ITEM_PARAGRAPH),
        ),
        "attributes" to mapOf(
            "attributes-keep" to DataKeyEnumerationOption(mapOf(
                "last" to DataKeyKeepType(AttributesExtension.ATTRIBUTES_KEEP, KeepType.LAST),
                "first" to DataKeyKeepType(AttributesExtension.ATTRIBUTES_KEEP, KeepType.FIRST),
                "fail" to DataKeyKeepType(AttributesExtension.ATTRIBUTES_KEEP, KeepType.FAIL),
                "locked" to DataKeyKeepType(AttributesExtension.ATTRIBUTES_KEEP, KeepType.LOCKED)
            )),
            "assign-text-attributes" to DataKeyBoolean(AttributesExtension.ASSIGN_TEXT_ATTRIBUTES),
            "fenced-code-info-attributes" to DataKeyBoolean(AttributesExtension.FENCED_CODE_INFO_ATTRIBUTES),
            "fenced-code-add-attributes" to DataKeyEnumerationOption(mapOf(
                "add-to-pre-code" to DataKeyFencedCodeAddType(AttributesExtension.FENCED_CODE_ADD_ATTRIBUTES, FencedCodeAddType.ADD_TO_PRE_CODE),
                "add-to-pre" to DataKeyFencedCodeAddType(AttributesExtension.FENCED_CODE_ADD_ATTRIBUTES, FencedCodeAddType.ADD_TO_PRE),
                "add-to-code" to DataKeyFencedCodeAddType(AttributesExtension.FENCED_CODE_ADD_ATTRIBUTES, FencedCodeAddType.ADD_TO_CODE)
            )),
            "wrap-non-attribute-text" to DataKeyBoolean(AttributesExtension.WRAP_NON_ATTRIBUTE_TEXT),
            "use-empty-implicit-as-span-delimiter" to DataKeyBoolean(AttributesExtension.USE_EMPTY_IMPLICIT_AS_SPAN_DELIMITER),
            "format-attributes-combine-consecutive" to DataKeyBoolean(AttributesExtension.FORMAT_ATTRIBUTES_COMBINE_CONSECUTIVE),
            "format-attributes-sort" to DataKeyBoolean(AttributesExtension.FORMAT_ATTRIBUTES_SORT),
            "format-attributes-spaces" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyDiscretionaryText(AttributesExtension.FORMAT_ATTRIBUTES_SPACES, DiscretionaryText.AS_IS),
                "add" to DataKeyDiscretionaryText(AttributesExtension.FORMAT_ATTRIBUTES_SPACES, DiscretionaryText.ADD),
                "remove" to DataKeyDiscretionaryText(AttributesExtension.FORMAT_ATTRIBUTES_SPACES, DiscretionaryText.REMOVE)
            )),
            "format-attribute-equal-space" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyDiscretionaryText(AttributesExtension.FORMAT_ATTRIBUTE_EQUAL_SPACE, DiscretionaryText.AS_IS),
                "add" to DataKeyDiscretionaryText(AttributesExtension.FORMAT_ATTRIBUTE_EQUAL_SPACE, DiscretionaryText.ADD),
                "remove" to DataKeyDiscretionaryText(AttributesExtension.FORMAT_ATTRIBUTE_EQUAL_SPACE, DiscretionaryText.REMOVE)
            )),
            "format-attribute-value-quotes" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyAttributeValueQuotes(AttributesExtension.FORMAT_ATTRIBUTE_VALUE_QUOTES, AttributeValueQuotes.AS_IS),
                "no-quotes-single-preferred" to DataKeyAttributeValueQuotes(AttributesExtension.FORMAT_ATTRIBUTE_VALUE_QUOTES, AttributeValueQuotes.NO_QUOTES_SINGLE_PREFERRED),
                "no-quotes-double-preferred" to DataKeyAttributeValueQuotes(AttributesExtension.FORMAT_ATTRIBUTE_VALUE_QUOTES, AttributeValueQuotes.NO_QUOTES_DOUBLE_PREFERRED),
                "single-preferred" to DataKeyAttributeValueQuotes(AttributesExtension.FORMAT_ATTRIBUTE_VALUE_QUOTES, AttributeValueQuotes.SINGLE_PREFERRED),
                "double-preferred" to DataKeyAttributeValueQuotes(AttributesExtension.FORMAT_ATTRIBUTE_VALUE_QUOTES, AttributeValueQuotes.DOUBLE_PREFERRED),
                "single-quotes" to DataKeyAttributeValueQuotes(AttributesExtension.FORMAT_ATTRIBUTE_VALUE_QUOTES, AttributeValueQuotes.SINGLE_QUOTES),
                "double-quotes" to DataKeyAttributeValueQuotes(AttributesExtension.FORMAT_ATTRIBUTE_VALUE_QUOTES, AttributeValueQuotes.DOUBLE_QUOTES)
            )),
            "format-attribute-id" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyAttributeImplicitName(AttributesExtension.FORMAT_ATTRIBUTE_ID, AttributeImplicitName.AS_IS),
                "implicit-preferred" to DataKeyAttributeImplicitName(AttributesExtension.FORMAT_ATTRIBUTE_ID, AttributeImplicitName.IMPLICIT_PREFERRED),
                "explicit-preferred" to DataKeyAttributeImplicitName(AttributesExtension.FORMAT_ATTRIBUTE_ID, AttributeImplicitName.EXPLICIT_PREFERRED)
            )),
            "format-attribute-class" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyAttributeImplicitName(AttributesExtension.FORMAT_ATTRIBUTE_CLASS, AttributeImplicitName.AS_IS),
                "implicit-preferred" to DataKeyAttributeImplicitName(AttributesExtension.FORMAT_ATTRIBUTE_CLASS, AttributeImplicitName.IMPLICIT_PREFERRED),
                "explicit-preferred" to DataKeyAttributeImplicitName(AttributesExtension.FORMAT_ATTRIBUTE_CLASS, AttributeImplicitName.EXPLICIT_PREFERRED)
            )),
        ),
        "autolink" to mapOf(
            "ignore-links" to DataKeyString(AutolinkExtension.IGNORE_LINKS),
        ),
        "definition" to mapOf(
            "colon-marker" to DataKeyBoolean(DefinitionExtension.COLON_MARKER),
            "marker-spaces" to DataKeyInteger(DefinitionExtension.MARKER_SPACES),
            "tilde-marker" to DataKeyBoolean(DefinitionExtension.TILDE_MARKER),
            "double-blank-line-breaks-list" to DataKeyBoolean(DefinitionExtension.DOUBLE_BLANK_LINE_BREAKS_LIST),
            "format-marker-spaces" to DataKeyInteger(DefinitionExtension.FORMAT_MARKER_SPACES),
            "format-marker-type" to DataKeyEnumerationOption(mapOf(
                "any" to DataKeyDefinitionMarker(DefinitionExtension.FORMAT_MARKER_TYPE, DefinitionMarker.ANY),
                "colon" to DataKeyDefinitionMarker(DefinitionExtension.FORMAT_MARKER_TYPE, DefinitionMarker.COLON),
                "tilde" to DataKeyDefinitionMarker(DefinitionExtension.FORMAT_MARKER_TYPE, DefinitionMarker.TILDE)
            )),
        ),
        "emoji" to mapOf(
            "root-image-path" to DataKeyString(EmojiExtension.ROOT_IMAGE_PATH),
            "use-shortcut-type" to DataKeyEnumerationOption(mapOf(
                "github" to DataKeyEmojiShortcutType(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.GITHUB),
                "emoji-cheat-sheet" to DataKeyEmojiShortcutType(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.EMOJI_CHEAT_SHEET),
                "any-github-preferred" to DataKeyEmojiShortcutType(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.ANY_GITHUB_PREFERRED),
                "any-emoji-cheat-sheet-preferred" to DataKeyEmojiShortcutType(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.ANY_EMOJI_CHEAT_SHEET_PREFERRED)
            )),
            "use-image-type" to DataKeyEnumerationOption(mapOf(
                "image-only" to DataKeyEmojiImageType(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.IMAGE_ONLY),
                "unicode-only" to DataKeyEmojiImageType(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY),
                "unicode-fallback-to-image" to DataKeyEmojiImageType(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_FALLBACK_TO_IMAGE)
            )),
            "use-unicode-file-names" to DataKeyBoolean(EmojiExtension.USE_UNICODE_FILE_NAMES),
            "attr-image-size" to DataKeyString(EmojiExtension.ATTR_IMAGE_SIZE),
            "attr-image-class" to DataKeyString(EmojiExtension.ATTR_IMAGE_CLASS),
            "attr-align" to DataKeyString(EmojiExtension.ATTR_ALIGN),
        ),
        "enumeratedreference" to mapOf(
            "enumerated-references-keep" to DataKeyEnumerationOption(mapOf(
                "last" to DataKeyKeepType(EnumeratedReferenceExtension.ENUMERATED_REFERENCES_KEEP, KeepType.LAST),
                "first" to DataKeyKeepType(EnumeratedReferenceExtension.ENUMERATED_REFERENCES_KEEP, KeepType.FIRST),
                "fail" to DataKeyKeepType(EnumeratedReferenceExtension.ENUMERATED_REFERENCES_KEEP, KeepType.FAIL),
                "locked" to DataKeyKeepType(EnumeratedReferenceExtension.ENUMERATED_REFERENCES_KEEP, KeepType.LOCKED)
            )),
            "enumerated-reference-placement" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyElementPlacement(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_PLACEMENT, ElementPlacement.AS_IS),
                "document-top" to DataKeyElementPlacement(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_PLACEMENT, ElementPlacement.DOCUMENT_TOP),
                "group-with-first" to DataKeyElementPlacement(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_PLACEMENT, ElementPlacement.GROUP_WITH_FIRST),
                "group-with-last" to DataKeyElementPlacement(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_PLACEMENT, ElementPlacement.GROUP_WITH_LAST),
                "document-bottom" to DataKeyElementPlacement(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_PLACEMENT, ElementPlacement.DOCUMENT_BOTTOM)
            )),
            "enumerated-reference-sort" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyElementPlacementSort(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_SORT, ElementPlacementSort.AS_IS),
                "sort" to DataKeyElementPlacementSort(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_SORT, ElementPlacementSort.SORT),
                "sort-unused-last" to DataKeyElementPlacementSort(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_SORT, ElementPlacementSort.SORT_UNUSED_LAST),
                "sort-delete-unused" to DataKeyElementPlacementSort(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_SORT, ElementPlacementSort.SORT_DELETE_UNUSED),
                "delete-unused" to DataKeyElementPlacementSort(EnumeratedReferenceExtension.ENUMERATED_REFERENCE_SORT, ElementPlacementSort.DELETE_UNUSED)
            )),
        ),
        "escaped-character" to mapOf(
        ),
        "footnotes" to mapOf(
            "footnotes-keep" to DataKeyEnumerationOption(mapOf(
                "last" to DataKeyKeepType(FootnoteExtension.FOOTNOTES_KEEP, KeepType.LAST),
                "first" to DataKeyKeepType(FootnoteExtension.FOOTNOTES_KEEP, KeepType.FIRST),
                "fail" to DataKeyKeepType(FootnoteExtension.FOOTNOTES_KEEP, KeepType.FAIL),
                "locked" to DataKeyKeepType(FootnoteExtension.FOOTNOTES_KEEP, KeepType.LOCKED)
            )),
            "footnote-ref-prefix" to DataKeyString(FootnoteExtension.FOOTNOTE_REF_PREFIX),
            "footnote-ref-suffix" to DataKeyString(FootnoteExtension.FOOTNOTE_REF_SUFFIX),
            "footnote-back-ref-string" to DataKeyString(FootnoteExtension.FOOTNOTE_BACK_REF_STRING),
            "footnote-link-ref-class" to DataKeyString(FootnoteExtension.FOOTNOTE_LINK_REF_CLASS),
            "footnote-back-link-ref-class" to DataKeyString(FootnoteExtension.FOOTNOTE_BACK_LINK_REF_CLASS),
            "footnote-placement" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyElementPlacement(FootnoteExtension.FOOTNOTE_PLACEMENT, ElementPlacement.AS_IS),
                "document-top" to DataKeyElementPlacement(FootnoteExtension.FOOTNOTE_PLACEMENT, ElementPlacement.DOCUMENT_TOP),
                "group-with-first" to DataKeyElementPlacement(FootnoteExtension.FOOTNOTE_PLACEMENT, ElementPlacement.GROUP_WITH_FIRST),
                "group-with-last" to DataKeyElementPlacement(FootnoteExtension.FOOTNOTE_PLACEMENT, ElementPlacement.GROUP_WITH_LAST),
                "document-bottom" to DataKeyElementPlacement(FootnoteExtension.FOOTNOTE_PLACEMENT, ElementPlacement.DOCUMENT_BOTTOM)
            )),
            "footnote-sort" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyElementPlacementSort(FootnoteExtension.FOOTNOTE_SORT, ElementPlacementSort.AS_IS),
                "sort" to DataKeyElementPlacementSort(FootnoteExtension.FOOTNOTE_SORT, ElementPlacementSort.SORT),
                "sort-unused-last" to DataKeyElementPlacementSort(FootnoteExtension.FOOTNOTE_SORT, ElementPlacementSort.SORT_UNUSED_LAST),
                "sort-delete-unused" to DataKeyElementPlacementSort(FootnoteExtension.FOOTNOTE_SORT, ElementPlacementSort.SORT_DELETE_UNUSED),
                "delete-unused" to DataKeyElementPlacementSort(FootnoteExtension.FOOTNOTE_SORT, ElementPlacementSort.DELETE_UNUSED)
            )),
        ),
        "gfm-issues" to mapOf(
            "git-hub-issues-url-root" to DataKeyString(GfmIssuesExtension.GIT_HUB_ISSUES_URL_ROOT),
            "git-hub-issue-url-prefix" to DataKeyString(GfmIssuesExtension.GIT_HUB_ISSUE_URL_PREFIX),
            "git-hub-issue-url-suffix" to DataKeyString(GfmIssuesExtension.GIT_HUB_ISSUE_URL_SUFFIX),
            "git-hub-issue-html-prefix" to DataKeyString(GfmIssuesExtension.GIT_HUB_ISSUE_HTML_PREFIX),
            "git-hub-issue-html-suffix" to DataKeyString(GfmIssuesExtension.GIT_HUB_ISSUE_HTML_SUFFIX),
        ),
        "gfm-strikethrough" to mapOf(
            "strikethrough-style-html-open" to DataKeyNullableString(StrikethroughExtension.STRIKETHROUGH_STYLE_HTML_OPEN),
            "strikethrough-style-html-close" to DataKeyNullableString(StrikethroughExtension.STRIKETHROUGH_STYLE_HTML_CLOSE),
        ),
        "gfm-strikethrough-subscript" to mapOf(
            "subscript-style-html-open" to DataKeyNullableString(SubscriptExtension.SUBSCRIPT_STYLE_HTML_OPEN),
            "subscript-style-html-close" to DataKeyNullableString(SubscriptExtension.SUBSCRIPT_STYLE_HTML_CLOSE),
        ),
        "gfm-strikethrough-strikethrough-subscript" to mapOf(
            "strikethrough-style-html-open" to DataKeyNullableString(StrikethroughSubscriptExtension.STRIKETHROUGH_STYLE_HTML_OPEN),
            "strikethrough-style-html-close" to DataKeyNullableString(StrikethroughSubscriptExtension.STRIKETHROUGH_STYLE_HTML_CLOSE),
            "subscript-style-html-open" to DataKeyNullableString(StrikethroughSubscriptExtension.SUBSCRIPT_STYLE_HTML_OPEN),
            "subscript-style-html-close" to DataKeyNullableString(StrikethroughSubscriptExtension.SUBSCRIPT_STYLE_HTML_CLOSE),
        ),
        "gfm-tasklist" to mapOf(
            "item-done-marker" to DataKeyString(TaskListExtension.ITEM_DONE_MARKER),
            "item-not-done-marker" to DataKeyString(TaskListExtension.ITEM_NOT_DONE_MARKER),
            "tight-item-class" to DataKeyString(TaskListExtension.TIGHT_ITEM_CLASS),
            "loose-item-class" to DataKeyString(TaskListExtension.LOOSE_ITEM_CLASS),
            "paragraph-class" to DataKeyString(TaskListExtension.PARAGRAPH_CLASS),
            "item-done-class" to DataKeyString(TaskListExtension.ITEM_DONE_CLASS),
            "item-not-done-class" to DataKeyString(TaskListExtension.ITEM_NOT_DONE_CLASS),
            "format-list-item-case" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyTaskListItemCase(TaskListExtension.FORMAT_LIST_ITEM_CASE, TaskListItemCase.AS_IS),
                "lowercase" to DataKeyTaskListItemCase(TaskListExtension.FORMAT_LIST_ITEM_CASE, TaskListItemCase.LOWERCASE),
                "uppercase" to DataKeyTaskListItemCase(TaskListExtension.FORMAT_LIST_ITEM_CASE, TaskListItemCase.UPPERCASE)
            )),
            "format-list-item-placement" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyTaskListItemPlacement(TaskListExtension.FORMAT_LIST_ITEM_PLACEMENT, TaskListItemPlacement.AS_IS),
                "incomplete-first" to DataKeyTaskListItemPlacement(TaskListExtension.FORMAT_LIST_ITEM_PLACEMENT, TaskListItemPlacement.INCOMPLETE_FIRST),
                "incomplete-nested-first" to DataKeyTaskListItemPlacement(TaskListExtension.FORMAT_LIST_ITEM_PLACEMENT, TaskListItemPlacement.INCOMPLETE_NESTED_FIRST),
                "complete-to-non-task" to DataKeyTaskListItemPlacement(TaskListExtension.FORMAT_LIST_ITEM_PLACEMENT, TaskListItemPlacement.COMPLETE_TO_NON_TASK),
                "complete-nested-to-non-task" to DataKeyTaskListItemPlacement(TaskListExtension.FORMAT_LIST_ITEM_PLACEMENT, TaskListItemPlacement.COMPLETE_NESTED_TO_NON_TASK)
            )),
            "format-ordered-task-item-priority" to DataKeyInteger(TaskListExtension.FORMAT_ORDERED_TASK_ITEM_PRIORITY),
            "format-default-task-item-priority" to DataKeyInteger(TaskListExtension.FORMAT_DEFAULT_TASK_ITEM_PRIORITY),
            "format-prioritized-task-items" to DataKeyBoolean(TaskListExtension.FORMAT_PRIORITIZED_TASK_ITEMS),
            "format-task-item-priorities" to DataKeyMapCharacterInteger(TaskListExtension.FORMAT_TASK_ITEM_PRIORITIES),
        ),
        "gfm-users" to mapOf(
            "git-hub-users-url-root" to DataKeyString(GfmUsersExtension.GIT_HUB_USERS_URL_ROOT),
            "git-hub-user-url-prefix" to DataKeyString(GfmUsersExtension.GIT_HUB_USER_URL_PREFIX),
            "git-hub-user-url-suffix" to DataKeyString(GfmUsersExtension.GIT_HUB_USER_URL_SUFFIX),
            "git-hub-user-html-prefix" to DataKeyString(GfmUsersExtension.GIT_HUB_USER_HTML_PREFIX),
            "git-hub-user-html-suffix" to DataKeyString(GfmUsersExtension.GIT_HUB_USER_HTML_SUFFIX),
        ),
        "gitlab" to mapOf(
            "ins-parser" to DataKeyBoolean(GitLabExtension.INS_PARSER),
            "del-parser" to DataKeyBoolean(GitLabExtension.DEL_PARSER),
            "block-quote-parser" to DataKeyBoolean(GitLabExtension.BLOCK_QUOTE_PARSER),
            "nested-block-quotes" to DataKeyBoolean(GitLabExtension.NESTED_BLOCK_QUOTES),
            "inline-math-parser" to DataKeyBoolean(GitLabExtension.INLINE_MATH_PARSER),
            "render-block-math" to DataKeyBoolean(GitLabExtension.RENDER_BLOCK_MATH),
            "render-block-mermaid" to DataKeyBoolean(GitLabExtension.RENDER_BLOCK_MERMAID),
            "render-video-images" to DataKeyBoolean(GitLabExtension.RENDER_VIDEO_IMAGES),
            "render-video-link" to DataKeyBoolean(GitLabExtension.RENDER_VIDEO_LINK),
            "math-languages" to DataKeyArrayString(GitLabExtension.MATH_LANGUAGES),
            "mermaid-languages" to DataKeyArrayString(GitLabExtension.MERMAID_LANGUAGES),
            "inline-math-class" to DataKeyString(GitLabExtension.INLINE_MATH_CLASS),
            "block-math-class" to DataKeyString(GitLabExtension.BLOCK_MATH_CLASS),
            "block-mermaid-class" to DataKeyString(GitLabExtension.BLOCK_MERMAID_CLASS),
            "video-image-class" to DataKeyString(GitLabExtension.VIDEO_IMAGE_CLASS),
            "video-image-link-text-format" to DataKeyString(GitLabExtension.VIDEO_IMAGE_LINK_TEXT_FORMAT),
        ),
        "ins" to mapOf(
            "ins-style-html-open" to DataKeyNullableString(InsExtension.INS_STYLE_HTML_OPEN),
            "ins-style-html-close" to DataKeyNullableString(InsExtension.INS_STYLE_HTML_CLOSE),
        ),
        "jekyll-front-matter" to mapOf(
        ),
        "macros" to mapOf(
            "macro-definitions-keep" to DataKeyEnumerationOption(mapOf(
                "last" to DataKeyKeepType(MacrosExtension.MACRO_DEFINITIONS_KEEP, KeepType.LAST),
                "first" to DataKeyKeepType(MacrosExtension.MACRO_DEFINITIONS_KEEP, KeepType.FIRST),
                "fail" to DataKeyKeepType(MacrosExtension.MACRO_DEFINITIONS_KEEP, KeepType.FAIL),
                "locked" to DataKeyKeepType(MacrosExtension.MACRO_DEFINITIONS_KEEP, KeepType.LOCKED)
            )),
            "macro-definitions-placement" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyElementPlacement(MacrosExtension.MACRO_DEFINITIONS_PLACEMENT, ElementPlacement.AS_IS),
                "document-top" to DataKeyElementPlacement(MacrosExtension.MACRO_DEFINITIONS_PLACEMENT, ElementPlacement.DOCUMENT_TOP),
                "group-with-first" to DataKeyElementPlacement(MacrosExtension.MACRO_DEFINITIONS_PLACEMENT, ElementPlacement.GROUP_WITH_FIRST),
                "group-with-last" to DataKeyElementPlacement(MacrosExtension.MACRO_DEFINITIONS_PLACEMENT, ElementPlacement.GROUP_WITH_LAST),
                "document-bottom" to DataKeyElementPlacement(MacrosExtension.MACRO_DEFINITIONS_PLACEMENT, ElementPlacement.DOCUMENT_BOTTOM)
            )),
            "macro-definitions-sort" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyElementPlacementSort(MacrosExtension.MACRO_DEFINITIONS_SORT, ElementPlacementSort.AS_IS),
                "sort" to DataKeyElementPlacementSort(MacrosExtension.MACRO_DEFINITIONS_SORT, ElementPlacementSort.SORT),
                "sort-unused-last" to DataKeyElementPlacementSort(MacrosExtension.MACRO_DEFINITIONS_SORT, ElementPlacementSort.SORT_UNUSED_LAST),
                "sort-delete-unused" to DataKeyElementPlacementSort(MacrosExtension.MACRO_DEFINITIONS_SORT, ElementPlacementSort.SORT_DELETE_UNUSED),
                "delete-unused" to DataKeyElementPlacementSort(MacrosExtension.MACRO_DEFINITIONS_SORT, ElementPlacementSort.DELETE_UNUSED)
            )),
            "source-wrap-macro-references" to DataKeyBoolean(MacrosExtension.SOURCE_WRAP_MACRO_REFERENCES),
        ),
        "media-tags" to mapOf(
        ),
        "resizable-image" to mapOf(
        ),
        "superscript" to mapOf(
            "superscript-style-html-open" to DataKeyNullableString(SuperscriptExtension.SUPERSCRIPT_STYLE_HTML_OPEN),
            "superscript-style-html-close" to DataKeyNullableString(SuperscriptExtension.SUPERSCRIPT_STYLE_HTML_CLOSE),
        ),
        "tables" to mapOf(
            "trim-cell-whitespace" to DataKeyBoolean(TablesExtension.TRIM_CELL_WHITESPACE),
            "min-separator-dashes" to DataKeyInteger(TablesExtension.MIN_SEPARATOR_DASHES),
            "max-header-rows" to DataKeyInteger(TablesExtension.MAX_HEADER_ROWS),
            "min-header-rows" to DataKeyInteger(TablesExtension.MIN_HEADER_ROWS),
            "append-missing-columns" to DataKeyBoolean(TablesExtension.APPEND_MISSING_COLUMNS),
            "discard-extra-columns" to DataKeyBoolean(TablesExtension.DISCARD_EXTRA_COLUMNS),
            "column-spans" to DataKeyBoolean(TablesExtension.COLUMN_SPANS),
            "header-separator-column-match" to DataKeyBoolean(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH),
            "class-name" to DataKeyString(TablesExtension.CLASS_NAME),
            "with-caption" to DataKeyBoolean(TablesExtension.WITH_CAPTION),
            "format-table-trim-cell-whitespace" to DataKeyBoolean(TablesExtension.FORMAT_TABLE_TRIM_CELL_WHITESPACE),
            "format-table-lead-trail-pipes" to DataKeyBoolean(TablesExtension.FORMAT_TABLE_LEAD_TRAIL_PIPES),
            "format-table-space-around-pipes" to DataKeyBoolean(TablesExtension.FORMAT_TABLE_SPACE_AROUND_PIPES),
            "format-table-adjust-column-width" to DataKeyBoolean(TablesExtension.FORMAT_TABLE_ADJUST_COLUMN_WIDTH),
            "format-table-apply-column-alignment" to DataKeyBoolean(TablesExtension.FORMAT_TABLE_APPLY_COLUMN_ALIGNMENT),
            "format-table-fill-missing-columns" to DataKeyBoolean(TablesExtension.FORMAT_TABLE_FILL_MISSING_COLUMNS),
            "format-table-fill-missing-min-column" to DataKeyNullableInteger(TablesExtension.FORMAT_TABLE_FILL_MISSING_MIN_COLUMN),
            "format-table-left-align-marker" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyDiscretionaryText(TablesExtension.FORMAT_TABLE_LEFT_ALIGN_MARKER, DiscretionaryText.AS_IS),
                "add" to DataKeyDiscretionaryText(TablesExtension.FORMAT_TABLE_LEFT_ALIGN_MARKER, DiscretionaryText.ADD),
                "remove" to DataKeyDiscretionaryText(TablesExtension.FORMAT_TABLE_LEFT_ALIGN_MARKER, DiscretionaryText.REMOVE)
            )),
            "format-table-min-separator-column-width" to DataKeyInteger(TablesExtension.FORMAT_TABLE_MIN_SEPARATOR_COLUMN_WIDTH),
            "format-table-min-separator-dashes" to DataKeyInteger(TablesExtension.FORMAT_TABLE_MIN_SEPARATOR_DASHES),
            "format-table-caption" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyTableCaptionHandling(TablesExtension.FORMAT_TABLE_CAPTION, TableCaptionHandling.AS_IS),
                "add" to DataKeyTableCaptionHandling(TablesExtension.FORMAT_TABLE_CAPTION, TableCaptionHandling.ADD),
                "remove-empty" to DataKeyTableCaptionHandling(TablesExtension.FORMAT_TABLE_CAPTION, TableCaptionHandling.REMOVE_EMPTY),
                "remove" to DataKeyTableCaptionHandling(TablesExtension.FORMAT_TABLE_CAPTION, TableCaptionHandling.REMOVE)
            )),
            "format-table-caption-spaces" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeyDiscretionaryText(TablesExtension.FORMAT_TABLE_CAPTION_SPACES, DiscretionaryText.AS_IS),
                "add" to DataKeyDiscretionaryText(TablesExtension.FORMAT_TABLE_CAPTION_SPACES, DiscretionaryText.ADD),
                "remove" to DataKeyDiscretionaryText(TablesExtension.FORMAT_TABLE_CAPTION_SPACES, DiscretionaryText.REMOVE)
            )),
            "format-table-indent-prefix" to DataKeyString(TablesExtension.FORMAT_TABLE_INDENT_PREFIX),
        ),
        "sim-toc" to mapOf(
            "levels" to DataKeyInteger(SimTocExtension.LEVELS),
            "is-text-only" to DataKeyBoolean(SimTocExtension.IS_TEXT_ONLY),
            "is-numbered" to DataKeyBoolean(SimTocExtension.IS_NUMBERED),
            "list-type" to DataKeyEnumerationOption(mapOf(
                "hierarchy" to DataKeyTocOptionsListType(SimTocExtension.LIST_TYPE, TocOptions.ListType.HIERARCHY),
                "flat" to DataKeyTocOptionsListType(SimTocExtension.LIST_TYPE, TocOptions.ListType.FLAT),
                "flat-reversed" to DataKeyTocOptionsListType(SimTocExtension.LIST_TYPE, TocOptions.ListType.FLAT_REVERSED),
                "sorted" to DataKeyTocOptionsListType(SimTocExtension.LIST_TYPE, TocOptions.ListType.SORTED),
                "sorted-reversed" to DataKeyTocOptionsListType(SimTocExtension.LIST_TYPE, TocOptions.ListType.SORTED_REVERSED)
            )),
            "is-html" to DataKeyBoolean(SimTocExtension.IS_HTML),
            "title-level" to DataKeyInteger(SimTocExtension.TITLE_LEVEL),
            "title" to DataKeyNullableString(SimTocExtension.TITLE),
            "ast-include-options" to DataKeyBoolean(SimTocExtension.AST_INCLUDE_OPTIONS),
            "blank-line-spacer" to DataKeyBoolean(SimTocExtension.BLANK_LINE_SPACER),
            "div-class" to DataKeyString(SimTocExtension.DIV_CLASS),
            "list-class" to DataKeyString(SimTocExtension.LIST_CLASS),
            "case-sensitive-toc-tag" to DataKeyBoolean(SimTocExtension.CASE_SENSITIVE_TOC_TAG),
            "format-update-on-format" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeySimTocGenerateOnFormat(SimTocExtension.FORMAT_UPDATE_ON_FORMAT, SimTocGenerateOnFormat.AS_IS),
                "update" to DataKeySimTocGenerateOnFormat(SimTocExtension.FORMAT_UPDATE_ON_FORMAT, SimTocGenerateOnFormat.UPDATE),
                "remove" to DataKeySimTocGenerateOnFormat(SimTocExtension.FORMAT_UPDATE_ON_FORMAT, SimTocGenerateOnFormat.REMOVE)
            )),
        ),
        "toc" to mapOf(
            "levels" to DataKeyInteger(TocExtension.LEVELS),
            "is-text-only" to DataKeyBoolean(TocExtension.IS_TEXT_ONLY),
            "is-numbered" to DataKeyBoolean(TocExtension.IS_NUMBERED),
            "list-type" to DataKeyEnumerationOption(mapOf(
                "hierarchy" to DataKeyTocOptionsListType(TocExtension.LIST_TYPE, TocOptions.ListType.HIERARCHY),
                "flat" to DataKeyTocOptionsListType(TocExtension.LIST_TYPE, TocOptions.ListType.FLAT),
                "flat-reversed" to DataKeyTocOptionsListType(TocExtension.LIST_TYPE, TocOptions.ListType.FLAT_REVERSED),
                "sorted" to DataKeyTocOptionsListType(TocExtension.LIST_TYPE, TocOptions.ListType.SORTED),
                "sorted-reversed" to DataKeyTocOptionsListType(TocExtension.LIST_TYPE, TocOptions.ListType.SORTED_REVERSED)
            )),
            "is-html" to DataKeyBoolean(TocExtension.IS_HTML),
            "title-level" to DataKeyInteger(TocExtension.TITLE_LEVEL),
            "title" to DataKeyNullableString(TocExtension.TITLE),
            "ast-include-options" to DataKeyBoolean(TocExtension.AST_INCLUDE_OPTIONS),
            "blank-line-spacer" to DataKeyBoolean(TocExtension.BLANK_LINE_SPACER),
            "div-class" to DataKeyString(TocExtension.DIV_CLASS),
            "list-class" to DataKeyString(TocExtension.LIST_CLASS),
            "case-sensitive-toc-tag" to DataKeyBoolean(TocExtension.CASE_SENSITIVE_TOC_TAG),
            "format-update-on-format" to DataKeyEnumerationOption(mapOf(
                "as-is" to DataKeySimTocGenerateOnFormat(TocExtension.FORMAT_UPDATE_ON_FORMAT, SimTocGenerateOnFormat.AS_IS),
                "update" to DataKeySimTocGenerateOnFormat(TocExtension.FORMAT_UPDATE_ON_FORMAT, SimTocGenerateOnFormat.UPDATE),
                "remove" to DataKeySimTocGenerateOnFormat(TocExtension.FORMAT_UPDATE_ON_FORMAT, SimTocGenerateOnFormat.REMOVE)
            )),
        ),
        "typographic" to mapOf(
            "enable-quotes" to DataKeyBoolean(TypographicExtension.ENABLE_QUOTES),
            "enable-smarts" to DataKeyBoolean(TypographicExtension.ENABLE_SMARTS),
            "angle-quote-close" to DataKeyString(TypographicExtension.ANGLE_QUOTE_CLOSE),
            "angle-quote-open" to DataKeyString(TypographicExtension.ANGLE_QUOTE_OPEN),
            "angle-quote-unmatched" to DataKeyNullableString(TypographicExtension.ANGLE_QUOTE_UNMATCHED),
            "double-quote-close" to DataKeyString(TypographicExtension.DOUBLE_QUOTE_CLOSE),
            "double-quote-open" to DataKeyString(TypographicExtension.DOUBLE_QUOTE_OPEN),
            "double-quote-unmatched" to DataKeyNullableString(TypographicExtension.DOUBLE_QUOTE_UNMATCHED),
            "ellipsis" to DataKeyString(TypographicExtension.ELLIPSIS),
            "ellipsis-spaced" to DataKeyString(TypographicExtension.ELLIPSIS_SPACED),
            "em-dash" to DataKeyString(TypographicExtension.EM_DASH),
            "en-dash" to DataKeyString(TypographicExtension.EN_DASH),
            "single-quote-close" to DataKeyString(TypographicExtension.SINGLE_QUOTE_CLOSE),
            "single-quote-open" to DataKeyString(TypographicExtension.SINGLE_QUOTE_OPEN),
            "single-quote-unmatched" to DataKeyString(TypographicExtension.SINGLE_QUOTE_UNMATCHED),
        ),
        "wikilink" to mapOf(
            "allow-inlines" to DataKeyBoolean(WikiLinkExtension.ALLOW_INLINES),
            "allow-anchors" to DataKeyBoolean(WikiLinkExtension.ALLOW_ANCHORS),
            "allow-anchor-escape" to DataKeyBoolean(WikiLinkExtension.ALLOW_ANCHOR_ESCAPE),
            "allow-pipe-escape" to DataKeyBoolean(WikiLinkExtension.ALLOW_PIPE_ESCAPE),
            "disable-rendering" to DataKeyBoolean(WikiLinkExtension.DISABLE_RENDERING),
            "link-first-syntax" to DataKeyBoolean(WikiLinkExtension.LINK_FIRST_SYNTAX),
            "link-prefix" to DataKeyString(WikiLinkExtension.LINK_PREFIX),
            "link-prefix-absolute" to DataKeyString(WikiLinkExtension.LINK_PREFIX_ABSOLUTE),
            "image-prefix" to DataKeyString(WikiLinkExtension.IMAGE_PREFIX),
            "image-prefix-absolute" to DataKeyString(WikiLinkExtension.IMAGE_PREFIX_ABSOLUTE),
            "image-links" to DataKeyBoolean(WikiLinkExtension.IMAGE_LINKS),
            "link-file-extension" to DataKeyString(WikiLinkExtension.LINK_FILE_EXTENSION),
            "image-file-extension" to DataKeyString(WikiLinkExtension.IMAGE_FILE_EXTENSION),
            "link-escape-chars" to DataKeyString(WikiLinkExtension.LINK_ESCAPE_CHARS),
            "link-replace-chars" to DataKeyString(WikiLinkExtension.LINK_REPLACE_CHARS),
        ),
        "xwiki-macros" to mapOf(
            "enable-inline-macros" to DataKeyBoolean(MacroExtension.ENABLE_INLINE_MACROS),
            "enable-block-macros" to DataKeyBoolean(MacroExtension.ENABLE_BLOCK_MACROS),
            "enable-rendering" to DataKeyBoolean(MacroExtension.ENABLE_RENDERING),
        ),
        "yaml-front-matter" to mapOf(
        ),
        "youtube-embedded" to mapOf(
        ),
    )


    fun enable(extension: Parser.ParserExtension) {
        parserExtensions.add(extension)
    }

    abstract class DataKeyOption { }

    abstract class DataKeyEnumeration: DataKeyOption() {
        abstract fun set()
    }

    inner class DataKeyEnumerationOption(val mapping: Map<String,DataKeyEnumeration>): DataKeyOption() {
    }

    inner class DataKeyBoolean(val key: DataKey<Boolean>): DataKeyOption() {
        fun set(value: Boolean) {
            _options.set(key, value)
        }
    }

    inner class DataKeyString(val key: DataKey<String>): DataKeyOption() {
        fun set(value: String) {
            _options.set(key, value)
        }
    }

    inner class DataKeyNullableString(val key: NullableDataKey<String>): DataKeyOption() {
        fun set(value: String?) {
            _options.set(key, value)
        }
    }

    inner class DataKeyMapStringString(val key: DataKey<Map<String,String>>): DataKeyOption() {
        fun set(value: Map<String,String>) {
            _options.set(key, value)
        }
    }

    inner class DataKeyMapCharacterInteger(val key: DataKey<Map<Char,Int>>): DataKeyOption() {
        fun set(value: Map<Char,Int>) {
            _options.set(key, value)
        }
    }

    inner class DataKeyArrayString(val key: DataKey<Array<String>>): DataKeyOption() {
        fun set(value: Array<String>) {
            _options.set(key, value)
        }
    }

    inner class DataKeyInteger(val key: DataKey<Int>): DataKeyOption() {
        fun set(value: Int) {
            _options.set(key, value)
        }
    }

    inner class DataKeyNullableInteger(val key: NullableDataKey<Int>): DataKeyOption() {
        fun set(value: Int?) {
            _options.set(key, value)
        }
    }

    inner class DataKeyAttributeImplicitName(val key: DataKey<AttributeImplicitName>, val value: AttributeImplicitName): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyAttributeValueQuotes(val key: DataKey<AttributeValueQuotes>, val value: AttributeValueQuotes): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyDefinitionMarker(val key: DataKey<DefinitionMarker>, val value: DefinitionMarker): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyDiscretionaryText(val key: DataKey<DiscretionaryText>, val value: DiscretionaryText): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyElementPlacement(val key: DataKey<ElementPlacement>, val value: ElementPlacement): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyElementPlacementSort(val key: DataKey<ElementPlacementSort>, val value: ElementPlacementSort): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyEmojiImageType(val key: DataKey<EmojiImageType>, val value: EmojiImageType): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyEmojiShortcutType(val key: DataKey<EmojiShortcutType>, val value: EmojiShortcutType): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyFencedCodeAddType(val key: DataKey<FencedCodeAddType>, val value: FencedCodeAddType): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyKeepType(val key: DataKey<KeepType>, val value: KeepType): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeySimTocGenerateOnFormat(val key: DataKey<SimTocGenerateOnFormat>, val value: SimTocGenerateOnFormat): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyTableCaptionHandling(val key: DataKey<TableCaptionHandling>, val value: TableCaptionHandling): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyTaskListItemCase(val key: DataKey<TaskListItemCase>, val value: TaskListItemCase): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyTaskListItemPlacement(val key: DataKey<TaskListItemPlacement>, val value: TaskListItemPlacement): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
    inner class DataKeyTocOptionsListType(val key: DataKey<TocOptions.ListType>, val value: TocOptions.ListType): DataKeyEnumeration() {
        override fun set() {
            _options.set(key, value)
        }
    }
}
