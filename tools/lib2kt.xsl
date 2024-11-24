<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:f="https://nwalsh.com/ns/functions"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="text" encoding="utf-8"/>
<xsl:strip-space elements="*"/>

<xsl:variable name="nl" select="'&#10;'"/>

<xsl:template match="p:library">
  <xsl:apply-templates select="p:declare-step[@type]"/>
</xsl:template>

<xsl:template match="p:declare-step">
  <xsl:text>step = StepConstructor(context, {f:qname(@type)}){$nl}</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>{$nl}</xsl:text>
</xsl:template>  

<xsl:template match="p:input">
  <xsl:text>step.addInput("{@port}")</xsl:text>
  <xsl:if test="@primary">.withPrimary({if (@primary = 'true') then '' else @primary})</xsl:if>
  <xsl:if test="@sequence">.withSequence({if (@sequence = 'true') then '' else @sequence})</xsl:if>
  <xsl:if test="@content-types">
    <xsl:text>.withContentTypes("{@content-types}")</xsl:text>
  </xsl:if>
  <xsl:apply-templates/>
  <xsl:text>{$nl}</xsl:text>
</xsl:template>

<xsl:template match="p:output">
  <xsl:text>step.addOutput("{@port}")</xsl:text>
  <xsl:if test="@primary">.withPrimary({if (@primary = 'true') then '' else @primary})</xsl:if>
  <xsl:if test="@sequence">.withSequence({if (@sequence = 'true') then '' else @sequence})</xsl:if>
  <xsl:if test="@content-types">
    <xsl:text>.withContentTypes("{@content-types}")</xsl:text>
  </xsl:if>
  <xsl:apply-templates/>
  <xsl:text>{$nl}</xsl:text>
</xsl:template>

<xsl:template match="p:empty">
  <xsl:text>.withEmpty()</xsl:text>
</xsl:template>

<xsl:template match="p:option">
  <xsl:text>step.addOption({f:qname(@name)})</xsl:text>
  <xsl:if test="@as">.withAs("{@as}")</xsl:if>
  <xsl:if test="@values">.withValues("{@values}")</xsl:if>
  <xsl:if test="@static">.withStatic({if (@static = 'true') then '' else @static})</xsl:if>
  <xsl:if test="@required">.withRequired({if (@required = 'true') then '' else @required})</xsl:if>
  <xsl:if test="@select">.withSelect("{replace(@select, '&quot;', '\\&quot;') =&gt; replace('\$', '\\\$')}")</xsl:if>
  <xsl:text>{$nl}</xsl:text>
</xsl:template>

<xsl:template match="*">
  <xsl:message select="'Unsupported element: ', node-name(.))"/>
</xsl:template>

<xsl:function name="f:qname" as="xs:string">
  <xsl:param name="name" as="attribute()"/>
  <xsl:variable name="qname" select="resolve-QName($name, $name/parent::*)"/>

  <xsl:choose>
    <xsl:when test="not(contains($name, ':'))">QName("{$name}")</xsl:when>
    <xsl:when test="namespace-uri-from-QName($qname) = 'http://www.w3.org/ns/xproc'">
      <xsl:text>QName(NsP.namespace, "p:{local-name-from-QName($qname)}")</xsl:text>
    </xsl:when>
    <xsl:when test="namespace-uri-from-QName($qname) = 'http://xmlcalabash.com/ns/extensions'">
      <xsl:text>QName(NsCx.namespace, "cx:{local-name-from-QName($qname)}")</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>QName("{substring-before($name, ':')}", </xsl:text>
      <xsl:text>"{namespace-uri-from-QName($qname)}", </xsl:text>
      <xsl:text>"{local-name-from-QName($qname)}")</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

</xsl:stylesheet>
