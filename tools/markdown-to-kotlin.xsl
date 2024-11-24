<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:f="http://xmlcalabash.com/ns/functions"
                xmlns:md="https://xmlcalabash.com/ns/markdown-extensions"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:variable name="nl" select="'&#10;'"/>

<xsl:template match="md:extensions">
  <xsl:text>package com.xmlcalabash.util{$nl}{$nl}</xsl:text>
  <xsl:text>// This class is generated. Do not edit.{$nl}{$nl}</xsl:text>
  <xsl:variable name="imports" as="xs:string+">
    <xsl:sequence select="'import com.vladsch.flexmark.parser.Parser'"/>
    <xsl:sequence select="'import com.vladsch.flexmark.util.data.DataKey'"/>
    <xsl:sequence select="'import com.vladsch.flexmark.util.data.NullableDataKey'"/>
    <xsl:sequence select="'import com.vladsch.flexmark.util.data.MutableDataSet'"/>
    <xsl:sequence select="'import com.vladsch.flexmark.util.data.DataSet'"/>
    <xsl:for-each select="//*[@class]">
      <!-- Special case -->
      <xsl:choose>
        <xsl:when test="contains(@class, '.')">
          <xsl:text>import {ancestor-or-self::*[@package][1]/@package}.{substring-before(@class,'.')}</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>import {ancestor-or-self::*[@package][1]/@package}.{@class}</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:variable>

  <xsl:for-each select="distinct-values($imports)">
    <xsl:sort select="."/>
    <xsl:text>{.}{$nl}</xsl:text>
  </xsl:for-each>
  <xsl:text>{$nl}</xsl:text>

  <xsl:text>/** Generated class of options for p:markdown-to-html extensions.
 * &lt;p>These are the extensions for com.vladsch.flexmark:flexmark-all:0.64.8.&lt;/p>
 */{$nl}{$nl}</xsl:text>

  <xsl:text>class MarkdownExtensions() {{{$nl}</xsl:text>
  <xsl:text>    private val parserExtensions = mutableSetOf&lt;Parser.ParserExtension>(){$nl}</xsl:text>
  <xsl:text>    private val _options = MutableDataSet(){$nl}</xsl:text>
  <xsl:text>    private var once = true{$nl}</xsl:text>
  <xsl:text>    val options: MutableDataSet{$nl}</xsl:text>
  <xsl:text>        get() {{{$nl}</xsl:text>
  <xsl:text>            if (once &amp;&amp; parserExtensions.isNotEmpty()) {{{$nl}</xsl:text>
  <xsl:text>                _options.set(Parser.EXTENSIONS, parserExtensions.toList()){$nl}</xsl:text>
  <xsl:text>            }}{$nl}</xsl:text>
  <xsl:text>            once = false{$nl}</xsl:text>
  <xsl:text>            return _options{$nl}</xsl:text>
  <xsl:text>        }}{$nl}</xsl:text>
  <xsl:text>{$nl}</xsl:text>
  <xsl:text>    val extensions = mapOf({$nl}</xsl:text>
  <xsl:for-each select="md:extension">
    <xsl:text>        "{@name}" to {{ {@class}.create() }},{$nl}</xsl:text>
  </xsl:for-each>
  <xsl:text>    ){$nl}{$nl}</xsl:text>

  <xsl:text>    val extensionOptions = mapOf({$nl}</xsl:text>
  <xsl:apply-templates select="md:extension"/>
  <xsl:text>    ){$nl}</xsl:text>
  <xsl:text expand-text="no"><![CDATA[

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

  ]]></xsl:text>

  <xsl:for-each select="distinct-values(//md:option/@class)">
    <xsl:sort select="."/>
    <xsl:text>inner class DataKey{replace(., '\.', '')}(val key: DataKey&lt;{.}>, val value: {.}): DataKeyEnumeration() {{
  override fun set() {{
    _options.set(key, value)
  }}
}}{$nl}</xsl:text>
  </xsl:for-each>
  <xsl:text>}}{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:extension">
  <xsl:text>        "{@name}" to mapOf({$nl}</xsl:text>
  <xsl:apply-templates select="md:option"/>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[@type='boolean']">
  <xsl:text>            "{f:name(@enum)}" to DataKeyBoolean({../@class}.{@enum}),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[not(@type) or @type='string']">
  <xsl:text>            "{f:name(@enum)}" to DataKeyString({../@class}.{@enum}),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[(not(@type) or @type='string') and @nullable='true']" priority="25">
  <xsl:text>            "{f:name(@enum)}" to DataKeyNullableString({../@class}.{@enum}),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[@type='integer']">
  <xsl:text>            "{f:name(@enum)}" to DataKeyInteger({../@class}.{@enum}),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[@type='integer' and @nullable='true']" priority="25">
  <xsl:text>            "{f:name(@enum)}" to DataKeyNullableInteger({../@class}.{@enum}),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[@type='DataKeyMap:string,string']" priority="25">
  <xsl:text>            "{f:name(@enum)}" to DataKeyMapStringString({../@class}.{@enum}),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[@type='DataKeyMap:character,integer']" priority="25">
  <xsl:text>            "{f:name(@enum)}" to DataKeyMapCharacterInteger({../@class}.{@enum}),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[@type='DataKeyArray:string']" priority="25">
  <xsl:text>            "{f:name(@enum)}" to DataKeyArrayString({../@class}.{@enum}),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option[@class]" priority="30">
  <xsl:text>            "{f:name(@enum)}" to DataKeyEnumerationOption(mapOf({$nl}</xsl:text>

  <xsl:variable name="pclass" select="../@class"/>
  <xsl:variable name="this" select="."/>
  <xsl:for-each select="tokenize(@values, '\s+')">
    <xsl:text>                "{f:name(.)}" to DataKey{replace($this/@class, '\.', '')}({$pclass}.{$this/@enum}, {$this/@class}.{.})</xsl:text>
    <xsl:text>{if(position() = last()) then '' else ','}{$nl}</xsl:text>
  </xsl:for-each>

  <xsl:text>            )),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="md:option">
  <xsl:message>Unexpected: {serialize(.)}</xsl:message>
</xsl:template>

<xsl:function name="f:name" as="xs:string">
  <xsl:param name="enum" as="xs:string"/>
  <xsl:sequence select="lower-case($enum) => replace('_', '-')"/>
</xsl:function>

</xsl:stylesheet>
