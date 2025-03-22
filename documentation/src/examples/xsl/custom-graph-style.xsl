<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                xmlns:g="http://xmlcalabash.com/ns/description"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">
<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="g:port[@primary='true']">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:attribute name="h:bgcolor" select="'#ccccff'"/>
    <xsl:apply-templates select="node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="g:edge[@implicit = 'true']">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:attribute name="dot:color" select="'#8a8a8a'"/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
