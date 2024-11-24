<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://docbook.org/ns/docbook"
                exclude-result-prefixes="db xs"
                version="3.0">

<!--
    This is a "one off" stylesheet that combines some elements of the
    prose from the standard step descriptions with the refentry elements
    in the reference.
-->

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:variable name="steps" select="doc('../../build/steps.xml')"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="/">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="db:refentry">
  <refentry>
    <xsl:apply-templates select="@*,node()"/>
  </refentry>
</xsl:template>

<xsl:template match="db:refsynopsisdiv">
  <xsl:element name="{node-name(.)}" namespace="{namespace-uri(.)}">
    <xsl:variable name="stepid" select="replace(ancestor::db:refentry/@xml:id, '^p-', 'c.')"/>
    <xsl:variable name="step" select="$steps/db:steps/db:section[@xml:id=$stepid]"/>

    <xsl:text>&#10;</xsl:text>
    <xsl:text>&#10;</xsl:text>
    <refsection role="introduction">
      <xsl:text>&#10;</xsl:text>
      <title>Introduction</title>
      <xsl:text>&#10;</xsl:text>
      <xsl:apply-templates select="($step/p:declare-step/preceding-sibling::*)
                                    except ($step/db:info | $step/db:title)"/>
      <xsl:text>&#10;</xsl:text>
    </refsection>

    <xsl:text>&#10;</xsl:text>
    <xsl:text>&#10;</xsl:text>
    <refsection role="step-declaration">
      <xsl:text>&#10;</xsl:text>
      <title>Step declaration</title>
      <xsl:text>&#10;</xsl:text>
      <xsl:apply-templates select="@*,*"/>
      <xsl:text>&#10;</xsl:text>
    </refsection>

    <xsl:variable name="errors" as="element()?">
      <xsl:where-populated>
        <p:errors>
          <xsl:text>&#10;</xsl:text>
          <xsl:apply-templates select="$step//db:error"/>
        </p:errors>
      </xsl:where-populated>
    </xsl:variable>

    <xsl:if test="exists($errors)">
      <xsl:text>&#10;</xsl:text>
      <xsl:text>&#10;</xsl:text>
      <refsection role="errors">
        <title>Errors</title>
        <para>
          <xsl:for-each select="$errors/db:error">
            <xsl:text>&#10;</xsl:text>
            <xsl:apply-templates select="."/>
          </xsl:for-each>
          <xsl:text>&#10;</xsl:text>
        </para>
        <xsl:text>&#10;</xsl:text>
      </refsection>
    </xsl:if>

    <xsl:variable name="impls" as="element()?">
      <xsl:where-populated>
        <p:impl>
          <xsl:text>&#10;</xsl:text>
          <xsl:apply-templates select="$step//db:impl"/>
        </p:impl>
      </xsl:where-populated>
    </xsl:variable>

    <xsl:if test="exists($impls)">
      <xsl:text>&#10;</xsl:text>
      <xsl:text>&#10;</xsl:text>

      <refsection role="implementation-features">
        <title>Implementation-defined and -dependent features</title>
        <xsl:text>&#10;</xsl:text>
        <para>
          <xsl:for-each select="$impls/db:impl">
            <xsl:text>&#10;</xsl:text>
            <xsl:apply-templates select="."/>
          </xsl:for-each>
          <xsl:text>&#10;</xsl:text>
        </para>
        <xsl:text>&#10;</xsl:text>
      </refsection>
    </xsl:if>
    <xsl:text>&#10;</xsl:text>
  </xsl:element>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="*">
  <xsl:element name="{node-name(.)}" namespace="{namespace-uri(.)}">
    <xsl:apply-templates select="@*,node()"/>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>
