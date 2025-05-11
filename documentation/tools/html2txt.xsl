<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="text" encoding="utf-8" indent="no"/>
<xsl:strip-space elements="h:div h:body h:ul h:li"/>

<xsl:template match="/">
  <xsl:apply-templates select="/h:html/h:body/*"/>
</xsl:template>

<xsl:template match="h:div">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="h:p">
  <xsl:apply-templates/>
  <xsl:text>&#10;&#10;</xsl:text>
</xsl:template>

<xsl:template match="h:p/text() | h:code/text() | h:a/text()">
  <xsl:value-of select="replace(., '&#10;', ' ')"/>
</xsl:template>

<xsl:template match="h:ul|h:ol">
  <xsl:apply-templates/>
  <xsl:text>&#10;&#10;</xsl:text>
</xsl:template>

<xsl:template match="h:ul/h:li">
  <xsl:text>{count(preceding-sibling::h:li)+1}. </xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="h:ol/h:li">
  <xsl:text>* </xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="h:code">
  <xsl:text>`</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>`</xsl:text>
</xsl:template>

<xsl:template match="h:em">
  <xsl:text>*</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="h:span">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="h:a">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="h:a[@href]">
  <xsl:text>[</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>]({string(@href)}])</xsl:text>
</xsl:template>

<xsl:template match="text()">
  <xsl:value-of select="replace(., '&#10;', ' ')"/>
</xsl:template>

<xsl:template match="*">
  <xsl:message terminate="yes" select="'No template for ' || local-name(.)"/>
</xsl:template>

</xsl:stylesheet>
