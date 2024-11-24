<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://docbook.org/ns/docbook"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:variable name="steps" select="doc('../../build/steps.xml')"/>
<xsl:variable name="stepspec" select="'https://spec.xproc.org/master/head/steps/'"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="/" name="xsl:initial-template">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="p:declare-step" priority="100">
  <xsl:element name="{node-name(.)}" namespace="{namespace-uri(.)}">
    <xsl:apply-templates select="@*,node()"/>
    <xsl:variable name="id" select="ancestor::db:refentry[1]/@xml:id/string()"/>

    <xsl:if test="starts-with($id, 'p-')">
      <p:spec-fragments>
        <xsl:variable name="stepid" select="replace($id, '^p-', 'c.')"/>
        <xsl:attribute name="xml:id" select="'xproc.org-' || $stepid"/>
        <xsl:variable name="step" select="$steps/db:steps/db:section[@xml:id=$stepid]"/>
        <p:intro>
          <xsl:copy-of select="($step/p:declare-step/preceding-sibling::*)
                               except ($step/db:info | $step/db:title)"/>
        </p:intro>
        <xsl:where-populated>
          <p:errors>
            <xsl:apply-templates select="$step//db:error" mode="spec-frag"/>
          </p:errors>
        </xsl:where-populated>
        <xsl:where-populated>
          <p:impl>
            <xsl:apply-templates select="$step//db:impl" mode="spec-frag"/>
          </p:impl>
        </xsl:where-populated>
      </p:spec-fragments>
    </xsl:if>
  </xsl:element>
</xsl:template>

<xsl:template match="db:error" mode="spec-frag">
  <xsl:element name="error" namespace="http://docbook.org/ns/docbook">
    <xsl:copy-of select="@*"/>
    <xsl:attribute name="p:id" select="'err.' || @code"/>
    <xsl:copy-of select="node()" copy-namespaces="no"/>
  </xsl:element>
</xsl:template>

<xsl:template match="db:impl" mode="spec-frag">
  <xsl:element name="impl" namespace="http://docbook.org/ns/docbook">
    <xsl:copy-of select="@*"/>
    <xsl:attribute name="p:id" select="'impl-' || (count(preceding::db:impl)+1)"/>
    <xsl:copy-of select="node()" copy-namespaces="no"/>
  </xsl:element>
</xsl:template>

<xsl:template match="db:refsection[db:title = 'Description' and db:para = 'TBD.']">
  <xsl:message
      >Suppressing empty description for {replace(ancestor::db:refentry/@xml:id, '^p-', 'p:')}</xsl:message>
</xsl:template>

</xsl:stylesheet>
