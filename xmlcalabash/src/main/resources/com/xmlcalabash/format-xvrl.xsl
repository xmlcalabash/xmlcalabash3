<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xvrl="http://www.xproc.org/ns/xvrl"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:param name="encoding" static="yes" select="'utf-8'"/>
<xsl:output method="text" _encoding="{$encoding}" indent="no"/>

<xsl:variable name="nl" select="'&#10;'"/>

<xsl:template match="/">
  <xsl:apply-templates/>
  <xsl:text>{$nl}</xsl:text>
</xsl:template>

<xsl:template match="xvrl:reports">
  <xsl:apply-templates select="xvrl:metadata"/>
  <xsl:apply-templates select="xvrl:report"/>
</xsl:template>

<xsl:template match="xvrl:metadata">
  <xsl:choose>
    <xsl:when test="empty(../../..) and xvrl:timestamp">
      <xsl:apply-templates select="xvrl:timestamp"/>
    </xsl:when>
    <xsl:when test="../xvrl:reports">
      <xsl:text>XVRL Reports:{$nl}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>XVRL Report:{$nl}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates select="xvrl:schema, xvrl:document"/>
</xsl:template>

<xsl:template match="xvrl:timestamp">
  <xsl:choose>
    <xsl:when test=". castable as xs:dateTime">
      <xsl:variable name="dt"
                    select="format-dateTime(xs:dateTime(.), '[D01] [MNn,*-3] [Y0001] at [h01]:[m01]:[s01]Z')"/>
      <xsl:choose>
        <xsl:when test="../../self::xvrl:reports">
          <xsl:text>XVRL Reports on {$dt}</xsl:text>
          <xsl:apply-templates select="../xvrl:validator"/>
          <xsl:text>:{$nl}</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>XVRL Report on {$dt}</xsl:text>
          <xsl:apply-templates select="../xvrl:validator"/>
          <xsl:text>:{$nl}</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>XVRL report metadata has invalid date: {.}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="xvrl:validator">
  <xsl:text> from {@name/string()}, version {@version/string()}</xsl:text>
</xsl:template>

<xsl:template match="xvrl:schema">
  <xsl:if test="@href">
    <xsl:text>Validated with {@href/string()}{$nl}</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="xvrl:document">
  <xsl:if test="@href">
    <xsl:text>Validating {@href/string()}{$nl}</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="xvrl:report">
  <xsl:apply-templates select="xvrl:metadata"/>
  <xsl:apply-templates select="xvrl:detection"/>
</xsl:template>

<xsl:template match="xvrl:detection">
  <xsl:value-of select="upper-case(substring(@severity, 1, 1))"/>
  <xsl:value-of select="substring(@severity, 2)"/>
  <xsl:value-of select="@code ! (' ' || .)"/>
  <xsl:choose>
    <xsl:when test="xvrl:location">
      <xsl:apply-templates select="xvrl:location"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>:{$nl}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:choose>
    <xsl:when test="empty(xvrl:message)">  (No message provided)</xsl:when>
    <xsl:when test="empty(xvrl:message/*)">
      <xsl:text>  {xvrl:message/node()}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  {xvrl:message/node() ! serialize(., map{'method':'xml', 'indent':true()})}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>{$nl}</xsl:text>
</xsl:template>

<xsl:template match="xvrl:location">
  <xsl:text> at </xsl:text>
  <xsl:if test="@href">
    <xsl:value-of select="@href"/>
    <xsl:if test="@line or @column">:<xsl:value-of select="@line"/></xsl:if>
    <xsl:if test="@column">:<xsl:value-of select="@column"/></xsl:if>
    <xsl:text>{$nl}</xsl:text>
  </xsl:if>
  <xsl:value-of select="@xpath ! ('at XPath: ' || . || $nl)"/>
  <xsl:value-of select="@jsonpointer ! ('at JSON pointer: ' || . || $nl)"/>
  <xsl:value-of select="@jsonpath ! ('at JSONPath: ' || . || $nl)"/>
</xsl:template>

</xsl:stylesheet>
