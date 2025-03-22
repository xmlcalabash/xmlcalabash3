<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                xmlns:f="http://xmlcalabash.com/ns/functions"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                expand-text="yes"
                default-mode="dot-to-text"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:output method="text"/>
<xsl:key name="port" match="*" use="@port"/>

<xsl:variable name="nl" select="'&#10;'"/>

<xsl:template match="dot:digraph">
  <xsl:text>digraph pipeline</xsl:text>
  <xsl:text> {{&#10;</xsl:text>
  <xsl:text>compound=true{$nl}</xsl:text>
  <xsl:text>rankdir=TB{$nl}</xsl:text>
  <xsl:apply-templates select="." mode="properties"/>
  <xsl:apply-templates/>
  <xsl:text>}}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:subgraph[h:table]">
  <xsl:text>subgraph </xsl:text>
  <xsl:value-of select="@xml:id"/>
  <xsl:text> {{&#10;</xsl:text>
  <xsl:apply-templates select="." mode="properties"/>
  <xsl:text>node [shape=plaintext]{$nl}</xsl:text>
  <xsl:text>{@xml:id} [shape={(@shape,'plaintext')[1]};label=&lt;{$nl}</xsl:text>
  <xsl:value-of select="serialize(f:strip-ns(h:table), map{'method':'xml', 'indent':false()})"/>
  <xsl:text>{$nl}&gt;];{$nl}</xsl:text>
  <xsl:apply-templates select="* except h:table"/>
  <xsl:text>}}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:subgraph">
  <xsl:text>subgraph "</xsl:text>
  <xsl:value-of select="@xml:id"/>
  <xsl:text>" {{&#10;</xsl:text>
  <xsl:apply-templates select="." mode="properties"/>
  <xsl:apply-templates/>
  <xsl:text>}}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:digraph" mode="properties">
  <xsl:for-each select="@dot:*">
    <xsl:value-of select="local-name(.)"/>
    <xsl:text> = "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>";&#10;</xsl:text>
  </xsl:for-each>
</xsl:template>

<xsl:template match="dot:subgraph" mode="properties">
  <xsl:if test="not(@label)">
    <xsl:text>label = ""{$nl}</xsl:text>
  </xsl:if>

  <xsl:for-each select="@*">
    <xsl:value-of select="local-name(.)"/>
    <xsl:text> = "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>";&#10;</xsl:text>
  </xsl:for-each>
</xsl:template>

<xsl:template match="dot:input|dot:output">
  <xsl:text>{@xml:id} [</xsl:text>
  <xsl:for-each select="@label,@dot:*">
    <xsl:text>{local-name(.)}="{string(.)}";</xsl:text>
  </xsl:for-each>
  <xsl:text>]{$nl}</xsl:text>
</xsl:template>

<xsl:template match="dot:node">
  <xsl:text>{@xml:id}</xsl:text>
  <xsl:if test="@dot:*">
    <xsl:text> [</xsl:text>
    <xsl:for-each select="@dot:*">
      <xsl:text>{local-name(.)}={string(.)};</xsl:text>
    </xsl:for-each>
    <xsl:text>]</xsl:text>
  </xsl:if>
  <xsl:text>{$nl}</xsl:text>
</xsl:template>


<xsl:template match="dot:edge">
  <xsl:apply-templates select="@from" mode="port"/>
  <xsl:text> -&gt; </xsl:text>
  <xsl:apply-templates select="@to" mode="port"/>

  <xsl:if test="@dot:*">
    <xsl:text> [</xsl:text>
    <xsl:for-each select="@dot:*">
      <xsl:text>{local-name(.)}=</xsl:text>
      <xsl:choose>
        <xsl:when test="string(.) castable as xs:double">
          <xsl:text>{string(.)};</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>"{string(.)}";</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
    <xsl:text>]</xsl:text>
  </xsl:if>

  <xsl:text>{$nl}</xsl:text>
</xsl:template>

<xsl:template match="@to|@from" mode="port">
  <xsl:variable name="port" select="key('port', .)"/>
  <xsl:variable name="cluster" select="($port//ancestor::dot:subgraph)[last()]/@xml:id"/>
  <xsl:choose>
    <xsl:when test="empty($cluster)">
      <xsl:text>{.}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>{$cluster}:{.}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="dot:*">
  <xsl:message select="'Unexpected: ' || node-name(.)"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="f:strip-ns" as="element()">
  <xsl:param name="root" as="element()"/>
  <xsl:apply-templates select="$root" mode="strip-ns"/>
</xsl:function>

<xsl:template match="*" mode="strip-ns">
  <xsl:element name="{local-name(.)}">
    <xsl:apply-templates select="@*" mode="strip-ns"/>
    <xsl:apply-templates select="node()" mode="strip-ns"/>
  </xsl:element>
</xsl:template>

<xsl:template match="attribute()|text()" mode="strip-ns">
  <xsl:copy/>
</xsl:template>

<xsl:template match="@dot:*" mode="strip-ns">
  <xsl:attribute name="{local-name(.)}" select="string(.)"/>
</xsl:template>

<xsl:template match="text()[normalize-space(.) = '']" mode="strip-ns">
  <!-- discard -->
</xsl:template>

<xsl:template match="text()[parent::h:table or parent::h:tr]" priority="100" mode="strip-ns">
  <xsl:text>&#10;</xsl:text>
</xsl:template>

</xsl:stylesheet>
