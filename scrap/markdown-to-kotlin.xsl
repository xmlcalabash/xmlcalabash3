<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:f="http://xmlcalabash.com/ns/functions"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:variable name="nl" select="'&#10;'"/>

<xsl:template match="extensions">
  <xsl:text>package com.xmlcalabash.util{$nl}{$nl}</xsl:text>
  <xsl:text>// This class is generated. Do not edit.{$nl}{$nl}</xsl:text>
  <xsl:variable name="imports" as="xs:string+">
    <xsl:sequence select="'import com.vladsch.flexmark.parser.Parser'"/>
    <xsl:sequence select="'import com.vladsch.flexmark.util.data.DataKey'"/>
    <xsl:sequence select="'import com.vladsch.flexmark.util.data.NullableDataKey'"/>
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

  <xsl:text>class MarkdownExtensions {{{$nl}</xsl:text>
  <xsl:text>companion object {{{$nl}</xsl:text>
  <xsl:text>val description = listOf({$nl}</xsl:text>
  <xsl:apply-templates select="extension"/>
  <xsl:text>){$nl}</xsl:text>
  <xsl:text>}}{$nl}</xsl:text>

  <xsl:text><![CDATA[  class ExtensionDescription(
        name: String,
        parser: () -> Parser.ParserExtension,
        options: List<ExtensionOption>
    )

    abstract class ExtensionOption(
        val name: String
    )

    class ExtensionOptionEnum(
        name: String,
        val option: DataKey<*>,
        val values: Map<String,Enum<*>>
    ): ExtensionOption(name)

    class ExtensionOptionString(
        name: String,
        val option: DataKey<String>,
    ): ExtensionOption(name)

    class ExtensionOptionNullableString(
        name: String,
        val option: NullableDataKey<String>,
    ): ExtensionOption(name)

    class ExtensionOptionInteger(
        name: String,
        val option: DataKey<Int>,
    ): ExtensionOption(name)

    class ExtensionOptionNullableInteger(
        name: String,
        val option: NullableDataKey<Int>,
    ): ExtensionOption(name)

    class ExtensionOptionBoolean(
        name: String,
        val option: DataKey<Boolean>,
    ): ExtensionOption(name)

    class ExtensionOptionMapStringString(
        name: String,
        val option: DataKey<Map<String, String>>,
    ): ExtensionOption(name)

    class ExtensionOptionMapCharacterInteger(
        name: String,
        val option: DataKey<Map<Char, Int>>,
    ): ExtensionOption(name)

    class ExtensionOptionPlainMapCharacterInteger(
        name: String,
        val option: Map<Char, Int>,
    ): ExtensionOption(name)

    class ExtensionOptionArrayString(
        name: String,
        val option: DataKey<Array<String>>,
    ): ExtensionOption(name)

    class ExtensionOptionPlainArrayString(
        name: String,
        val option: Array<String>,
    ): ExtensionOption(name)
]]></xsl:text>

  <xsl:text>}}{$nl}</xsl:text>
</xsl:template>

<xsl:template match="extension">
  <xsl:text>ExtensionDescription({$nl}</xsl:text>
  <xsl:text>    "{@name}",{$nl}</xsl:text>
  <xsl:text>    {{ {@class}.create() }},{$nl}</xsl:text>
  <xsl:text>    listOf({$nl}</xsl:text>
  <xsl:apply-templates select="option"/>
  <xsl:text>    ){$nl}</xsl:text>
  <xsl:text>),{$nl}{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[(not(@type) or @type='string') and @nullable='true']" priority="12">
  <xsl:text>        ExtensionOptionNullableString({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[not(@type) or @type='string']" priority="10">
  <xsl:text>        ExtensionOptionString({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[@type='boolean']" priority="20">
  <xsl:text>        ExtensionOptionBoolean({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[@type='integer']" priority="20">
  <xsl:text>        ExtensionOptionInteger({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[@type='integer' and @nullable='true']" priority="25">
  <xsl:text>        ExtensionOptionNullableInteger({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[@type='DataKeyMap:string,string']" priority="20">
  <xsl:text>        ExtensionOptionMapStringString({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[@type='DataKeyMap:character,integer']" priority="20">
  <xsl:text>        ExtensionOptionMapCharacterInteger({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[@type='Map:character,integer']" priority="20">
  <xsl:text>        ExtensionOptionPlainMapCharacterInteger({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[@type='DataKeyArray:string']" priority="20">
  <xsl:text>        ExtensionOptionArrayString({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum}{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option[@class]" priority="20">
  <xsl:text>        ExtensionOptionEnum({$nl}</xsl:text>
  <xsl:text>            "{f:name(@enum)}",{$nl}</xsl:text>
  <xsl:text>            {../@class}.{@enum},{$nl}</xsl:text>
  <xsl:text>            mapOf({$nl}</xsl:text>
  
  <xsl:variable name="this" select="."/>
  <xsl:for-each select="tokenize(@values, '\s+')">
    <xsl:text>                "{f:name(.)}" to {$this/@class}.{.},{$nl}</xsl:text>
  </xsl:for-each>
  <xsl:text>            ),{$nl}</xsl:text>
  <xsl:text>        ),{$nl}</xsl:text>
</xsl:template>

<xsl:template match="option">
  <xsl:message>Unexpected: {serialize(.)}</xsl:message>
</xsl:template>

<xsl:function name="f:name" as="xs:string">
  <xsl:param name="enum" as="xs:string"/>
  <xsl:sequence select="lower-case($enum) => replace('_', '-')"/>
</xsl:function>

</xsl:stylesheet>
