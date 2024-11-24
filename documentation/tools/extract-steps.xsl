<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns="http://docbook.org/ns/docbook"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template name="xsl:initial-template">
  <steps xmlns="http://docbook.org/ns/docbook">
    <xsl:for-each select="collection('../build/specs?select=*.xml')">
      <xsl:apply-templates select=".//db:section[@xml:id]"/>
    </xsl:for-each>
  </steps>
</xsl:template>

<xsl:template match="db:section"/>

<xsl:template match="db:section[starts-with(@xml:id, 'c.')]">
  <xsl:sequence select="."/>
</xsl:template>

</xsl:stylesheet>
