<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
		exclude-result-prefixes="xs"
                version="2.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"
	    omit-xml-declaration="yes"/>

<xsl:template match="testsuite">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates select="properties">
      <xsl:sort select="@name"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="testcase">
      <xsl:sort select="@name"/>
    </xsl:apply-templates>
  </xsl:copy>
</xsl:template>

<xsl:template match="testcase">
  <testcase>
    <xsl:copy-of select="@name"/>
    <xsl:choose>
      <xsl:when test="failure">FAIL</xsl:when>
      <xsl:otherwise>PASS</xsl:otherwise>
    </xsl:choose>
  </testcase>
</xsl:template>

<xsl:template match="element()">
  <xsl:copy>
    <xsl:apply-templates select="@*,node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()">
  <xsl:copy/>
</xsl:template>

</xsl:stylesheet>
