<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:f="http://xmlcalabash.com/ns/functions"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                expand-text="yes"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:strip-space elements="*"/>

<xsl:variable name="nl" select="'&#10;'"/>

<xsl:template match="dot:digraph-wrapper">
  <!-- special case, mostly when debugging -->
  <xsl:text>digraph </xsl:text>
  <xsl:value-of select="@xml:id"/>
  <xsl:text> {{&#10;</xsl:text>
  <xsl:text>compound=true{$nl}</xsl:text>
  <xsl:text>rankdir=TB{$nl}</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:digraph">
  <!-- The digraph {} wrapper is added by the caller because
       it may be combining several graphs into one. -->
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="dot:subgraph[table]">
  <xsl:text>subgraph </xsl:text>
  <xsl:value-of select="@xml:id"/>
  <xsl:text> {{&#10;</xsl:text>
  <xsl:apply-templates select="." mode="properties"/>
  <xsl:text>node [shape=plaintext]{$nl}</xsl:text>
  <xsl:text>{@xml:id} [shape={(@shape,'plaintext')[1]};label=&lt;{$nl}</xsl:text>
  <xsl:value-of select="serialize(f:strip-ns(table), map{'method':'xml', 'indent':false()})"/>
  <xsl:text>{$nl}&gt;];{$nl}</xsl:text>
  <xsl:apply-templates select="* except table"/>
  <xsl:text>}}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:subgraph">
  <xsl:text>subgraph "</xsl:text>
  <xsl:value-of select="@xml:id"/>
  <xsl:text>" {{&#10;</xsl:text>
  <xsl:apply-templates select="." mode="properties"/>
  <xsl:for-each select="@*">
    <xsl:value-of select="local-name(.)"/>
    <xsl:text> = "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>";&#10;</xsl:text>
  </xsl:for-each>

  <xsl:apply-templates/>
  <xsl:text>}}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:subgraph" mode="properties">
  <xsl:if test="@peripheries">
    <xsl:text>peripheries = {@peripheries}{$nl}</xsl:text>
  </xsl:if>
  <xsl:choose>
    <xsl:when test="@label">
      <xsl:text>label = "{@label}"{$nl}</xsl:text>
      <xsl:if test="@labeljust">
        <xsl:text>labeljust={@labeljust}{$nl}</xsl:text>
      </xsl:if>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>label = ""{$nl}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
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
  <xsl:text>{@from}{if (string(@output) != '') then ':' || @output else ''} -> </xsl:text>
  <xsl:text>{@to}{if (string(@input) != '') then ':' || @input else ''}</xsl:text>

  <xsl:if test="@dot:*">
    <xsl:text> [</xsl:text>
    <xsl:for-each select="@dot:*">
      <xsl:text>{local-name(.)}={string(.)};</xsl:text>
    </xsl:for-each>
    <xsl:text>]</xsl:text>
  </xsl:if>

  <xsl:text>{$nl}</xsl:text>
</xsl:template>

<xsl:template match="dot:*">
  <xsl:message select="'Unexpected: ' || node-name(.)"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="f:strip-ns" as="element()">
  <xsl:param name="root" as="element()"/>
  <xsl:apply-templates select="$root" mode="strip-ns"/>
</xsl:function>

<xsl:mode name="strip-ns" on-no-match="shallow-copy"/>

<xsl:template match="*" mode="strip-ns">
  <xsl:element name="{local-name(.)}">
    <xsl:apply-templates select="@*,node()" mode="strip-ns"/>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>
