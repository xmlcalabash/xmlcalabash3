<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:dot="http://jafpl.com/ns/dot"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:strip-space elements="*"/>

<xsl:template match="dot:digraph">
  <xsl:text>digraph </xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text> {&#10;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:subgraph">
  <xsl:text>subgraph "</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>" {&#10;</xsl:text>

  <xsl:for-each select="@*">
    <xsl:value-of select="local-name(.)"/>
    <xsl:text> = "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>";&#10;</xsl:text>
  </xsl:for-each>

  <xsl:apply-templates/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:anchor">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>" [&#10;</xsl:text>
  <xsl:text>label = "</xsl:text>
  <xsl:value-of select="@label"/>
  <xsl:text>";&#10;</xsl:text>
  <xsl:text>];&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:arrow">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@from"/>
  <xsl:text>" -&gt; "</xsl:text>
  <xsl:value-of select="@to"/>
  <xsl:text>"</xsl:text>

  <xsl:variable name="labels" as="xs:string*"
                select="(@label,@headlabel,@taillabel) ! dot:label(.)"/>
  <xsl:if test="exists($labels)">
    <xsl:text> [</xsl:text>
    <xsl:sequence select="string-join($labels, '; ')"/>
    <xsl:text>]</xsl:text>
  </xsl:if>

  <xsl:text>;&#10;</xsl:text>
</xsl:template>

<xsl:function name="dot:label" as="xs:string?">
  <xsl:param name="attr" as="attribute()?"/>
  <xsl:if test="exists($attr)">
    <xsl:choose>
      <xsl:when test="local-name($attr) = 'headlabel' and $attr = 'source'"/>
      <xsl:when test="local-name($attr) = 'taillabel' and $attr = 'result'"/>
      <xsl:otherwise>
        <xsl:sequence select="local-name($attr) || '=&quot;' || $attr || '&quot;'"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:function>

<xsl:template match="dot:pipeline-input">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>" [&#10;</xsl:text>
  <xsl:for-each select="@* except (@name|@from)">
    <xsl:value-of select="local-name(.)"/>
    <xsl:text> = "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>";&#10;</xsl:text>
  </xsl:for-each>
  <xsl:text>];&#10;</xsl:text>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>" -> "</xsl:text>
  <xsl:value-of select="@from"/>
  <xsl:text>";&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:pipeline-output">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>" [&#10;</xsl:text>
  <xsl:for-each select="@* except (@name|@from)">
    <xsl:value-of select="local-name(.)"/>
    <xsl:text> = "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>";&#10;</xsl:text>
  </xsl:for-each>
  <xsl:text>];&#10;</xsl:text>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="@from"/>
  <xsl:text>" -> "</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>";&#10;</xsl:text>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()">
  <xsl:copy/>
</xsl:template>

</xsl:stylesheet>
