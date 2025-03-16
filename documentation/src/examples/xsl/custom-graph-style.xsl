<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:g="http://xmlcalabash.com/ns/description"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                version="3.0">

<xsl:import href="https://xmlcalabash.com/xsl/graphstyle.xsl"/>

<xsl:template match="g:port[@primary='true']">
  <xsl:copy>
    <xsl:attribute name="h:bgcolor" select="'#ccccff'"/>
    <xsl:apply-templates select="@*,node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="g:edge">
  <xsl:variable name="edge" as="element()">
    <xsl:next-match/>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$edge/@implicit = 'true'">
      <g:edge>
        <xsl:sequence select="$edge/@*"/>
        <xsl:attribute name="dot:color" select="'#8a8a8a'"/>
      </g:edge>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$edge"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
