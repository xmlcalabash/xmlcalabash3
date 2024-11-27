<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:nt="https://xmlcalabash.com/ns/nonterminal"
                xmlns:t="https://xmlcalabash.com/ns/terminal"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="text" encoding="utf-8" indent="no"/>

<xsl:template match="/">
  <xsl:variable name="context" as="xs:boolean*">
    <xsl:if test="nt:XPath/nt:QuantifiedExpr/nt:UnaryLookup">
      <xsl:sequence select="true()"/>
    </xsl:if>
    <xsl:apply-templates select="//nt:AxisStep"/>
    <xsl:apply-templates select="//nt:ContextItemExpr"/>
  </xsl:variable>
  <xsl:value-of select="'.' || (true() = $context)"/>
  <xsl:value-of select="'&#10;'"/>

  <xsl:variable name="functions" as="xs:string*">
    <xsl:apply-templates select="//nt:FunctionCall"/>
  </xsl:variable>
  <xsl:for-each select="distinct-values($functions)">
    <xsl:value-of select="'f' || . || '&#10;'"/>
  </xsl:for-each>

  <xsl:variable name="variables" as="xs:string*">
    <xsl:apply-templates select="//nt:VarRef"/>
  </xsl:variable>
  <xsl:for-each select="distinct-values($variables)">
    <xsl:value-of select="'v' || . || '&#10;'"/>
  </xsl:for-each>
</xsl:template>

<xsl:template match="nt:FunctionCall" as="xs:string?">
  <xsl:variable name="qname" select="t:QName"/>
  <xsl:variable name="inlinefunc"
                select="preceding::nt:InlineFunctionExpr/preceding-sibling::nt:VarName[1]/t:QName"/>

  <xsl:choose>
    <xsl:when test="$qname/@name = $inlinefunc/@name">
      <!-- there's a local declaration -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="t:QName/@name || '#' || count(nt:ArgumentList/*) || ' ' || string(t:QName)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="nt:VarRef" as="xs:string?">
  <xsl:variable name="qname" select="nt:VarName/t:QName"/>

  <xsl:variable name="quantified"
                select="ancestor::nt:QuantifiedExpr/nt:VarName/t:QName[@name = $qname/@name]"/>

  <xsl:variable name="letexpr"
                select="ancestor::nt:LetExpr//nt:SimpleLetBinding/nt:VarName/t:QName[@name = $qname/@name]"/>

  <xsl:variable name="inlinefunc"
                select="ancestor::nt:InlineFunctionExpr/nt:ParamList/nt:Param/t:QName[@name = $qname/@name]"/>

  <xsl:choose>
    <xsl:when test="some $v in $quantified satisfies $v/@name = $qname/@name and $v &lt;&lt; $qname">
      <!-- there's a local declaration -->
    </xsl:when>
    <xsl:when test="some $v in $letexpr satisfies $v/@name = $qname/@name and $v &lt;&lt; $qname">
      <!-- there's a local declaration -->
    </xsl:when>
    <xsl:when test="some $v in $inlinefunc satisfies $v/@name = $qname/@name and $v &lt;&lt; $qname">
      <!-- there's a local declaration -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$qname/@name/string() || ' ' || string($qname)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="nt:AxisStep" as="xs:boolean?">
  <xsl:choose>
    <xsl:when test="parent::nt:RelativePathExpr and preceding-sibling::nt:VarRef">
      <!-- not a reference to the context item -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="true()"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="nt:ContextItemExpr" as="xs:boolean?">
  <xsl:choose>
    <xsl:when test="ancestor::nt:PostfixExpr and preceding-sibling::nt:VarRef">
      <!-- not a reference to the context item -->
    </xsl:when>
    <xsl:when test="parent::nt:ComparisonExpr/preceding-sibling::nt:VarRef">
      <!-- not a reference to the context item -->
    </xsl:when>      
    <xsl:otherwise>
      <xsl:sequence select="true()"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="*">
  <xsl:message terminate="yes">Unexpected node: {node-name(.)}</xsl:message>
</xsl:template>

</xsl:stylesheet>
