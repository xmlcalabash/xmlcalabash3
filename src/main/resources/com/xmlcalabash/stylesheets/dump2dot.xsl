<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:dot="http://jafpl.com/ns/dot"
                xmlns:g="http://jafpl.com/ns/graph"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"
            omit-xml-declaration="yes"/>

<xsl:strip-space elements="*"/>

<xsl:key name="name" match="*" use="@name"/>
<xsl:key name="label" match="*" use="@label"/>
<xsl:key name="id" match="*" use="@id"/>

<xsl:template match="g:graph">
  <dot:digraph name="dump_graph">
    <xsl:apply-templates/>
    <xsl:apply-templates select="//g:outputs/g:out-edge" mode="pipes"/>
  </dot:digraph>
</xsl:template>

<xsl:template match="g:container">
  <dot:subgraph name="{g:name(.)}" label="{@label}" color="black">
    <xsl:apply-templates/>
  </dot:subgraph>
</xsl:template>

<xsl:template match="g:inputs|g:outputs">
</xsl:template>

<xsl:template match="g:children">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="g:node">
  <dot:anchor name="{g:name(.)}" label="{@label}" color="black">
    <xsl:apply-templates/>
  </dot:anchor>
</xsl:template>

<xsl:function name="g:name" as="xs:string">
  <xsl:param name="node" as="element()"/>
  <xsl:choose>
    <xsl:when test="local-name($node) = 'container'">
      <xsl:sequence
          select="'cluster_' || generate-id($node) || '-' || $node/@id"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence
          select="local-name($node) || '.' || generate-id($node) || '-' || $node/@id"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ============================================================ -->

<xsl:template match="g:out-edge" mode="pipes">
  <xsl:variable name="from" select="(ancestor::g:container|ancestor::g:node)[last()]"/>
  <xsl:variable name="to" select="key('id', @destination)"/>

  <dot:arrow from="{g:name($from)}" to="{g:name($to)}"
             headlabel="{@input-port}" taillabel="{@output-port}"/>
</xsl:template>

</xsl:stylesheet>
