<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:rng="http://relaxng.org/ns/structure/1.0"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:key name="define" match="rng:define" use="@name"/>

<xsl:output method="xml" encoding="utf-8" indent="yes"/>

<xsl:mode on-no-match="shallow-skip"/>

<xsl:template match="rng:grammar">
  <xsl:variable name="expanded" as="document-node()">
    <xsl:apply-templates select="/" mode="expand-refs"/>
  </xsl:variable>

<!--
  <xsl:result-document href="/tmp/expanded.xml">
    <xsl:sequence select="$expanded"/>
  </xsl:result-document>
-->

  <elements>
    <xsl:apply-templates select="$expanded/*/rng:define[ends-with(@name, '.element') and rng:element]"/>
  </elements>
</xsl:template>

<xsl:template match="rng:define">
  <xsl:apply-templates select="rng:element"/>
<!--
  <xsl:if test="@name = 'stylesheet.element'">
    <xsl:apply-templates select="rng:element"/>
  </xsl:if>
-->
</xsl:template>

<xsl:template match="rng:element[starts-with(@name, 'xs:')]"/>

<xsl:template match="rng:element">
  <element name="{@name}">
    <xsl:apply-templates select=".//rng:attribute[@name and not(parent::rng:optional)]"/>
    <xsl:apply-templates select=".//rng:attribute[@name and parent::rng:optional]"/>

    <xsl:variable name="group">
      <xsl:apply-templates select="rng:group"/>
      <xsl:apply-templates select="rng:ref[ends-with(@name, '.model')]" mode="group"/>
    </xsl:variable>

    <group>
      <xsl:for-each select="distinct-values($group//@name)">
        <xsl:if test=". != ''">
          <element name="{.}"/>
        </xsl:if>
      </xsl:for-each>
    </group>
  </element>
</xsl:template>

<xsl:template match="rng:attribute">
  <attribute name="{@name}" optional="{exists(parent::rng:optional)}">
    <xsl:choose>
      <xsl:when test="exists(.//*[local-name(.) != 'choice' and local-name(.) != 'value'])">
        <xsl:attribute name="type" select="'string'"/>
      </xsl:when>
      <xsl:when test=".//rng:value[. = 'yes']">
        <xsl:attribute name="type" select="'boolean'"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="distinct-values(.//rng:value/string())">
          <value><xsl:sequence select="."/></value>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </attribute>
</xsl:template>

<xsl:template match="rng:group">
  <xsl:apply-templates mode="group"/>
</xsl:template>

<xsl:template match="rng:zeroOrMore|rng:optional|rng:choice|rng:oneOrMore|rng:group
                     |rng:define" mode="group">
  <xsl:apply-templates mode="group"/>
</xsl:template>

<xsl:template match="rng:ref" mode="group">
  <ref>
    <xsl:apply-templates select="key('define', @name)" mode="group"/>
  </ref>
</xsl:template>

<xsl:template match="rng:element" mode="group">
  <element name="{@name}"/>
</xsl:template>

<xsl:template match="*" mode="group">
  <xsl:message select="'unexpected:', local-name(.)"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:mode name="expand-refs" on-no-match="shallow-copy"/>

<xsl:template match="/" mode="expand-refs">
  <xsl:document>
    <xsl:apply-templates select="*" mode="expand-refs">
      <xsl:with-param name="depth" select="0" tunnel="yes"/>
      <xsl:with-param name="attr" select="false()" tunnel="yes"/>
    </xsl:apply-templates>
  </xsl:document>
</xsl:template>

<xsl:template match="*" mode="expand-refs">
  <xsl:param name="depth" tunnel="yes"/>
  <xsl:param name="attr" tunnel="yes"/>

  <xsl:variable name="content" as="element()">
    <xsl:copy>
      <xsl:if test="not(@name) or not(starts-with(@name, '_'))">
        <xsl:apply-templates select="@*,*" mode="expand-refs"/>
      </xsl:if>
    </xsl:copy>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$attr or $content/node()">
      <xsl:sequence select="$content"/>
    </xsl:when>
    <xsl:when test="$content/self::rng:empty">
      <xsl:sequence select="$content"/>
    </xsl:when>
    <xsl:otherwise>
      <!--
      <xsl:sequence select="$content"/>
      -->
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="rng:value" mode="expand-refs">
  <xsl:param name="depth" tunnel="yes"/>
  <xsl:param name="attr" tunnel="yes"/>

  <xsl:copy-of select="."/>
</xsl:template>

<xsl:template match="rng:ref" mode="expand-refs">
  <xsl:param name="depth" tunnel="yes"/>
  <xsl:param name="attr" tunnel="yes"/>

  <xsl:choose>
    <xsl:when test="(ancestor::rng:attribute or $attr) and $depth lt 4">
      <xsl:apply-templates select="key('define', @name)/*" mode="expand-refs">
        <xsl:with-param name="depth" select="$depth+1" tunnel="yes"/>
        <xsl:with-param name="attr" select="true()" tunnel="yes"/>
      </xsl:apply-templates>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy>
        <xsl:apply-templates select="@*,*" mode="expand-refs"/>
      </xsl:copy>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
