<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:mp="http://docbook.org/ns/docbook/modes/private"
                xmlns:t="http://docbook.org/ns/docbook/templates"
                xmlns:tp="http://docbook.org/ns/docbook/templates/private"
                xmlns:v="http://docbook.org/ns/docbook/variables"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<!-- This is a horrifically cheap-and-cheerful stylesheet for processing
     stylized changelogs from DocBook to HTML. -->

<xsl:param name="version" as="xs:string" required="yes"/>

<xsl:template match="/">
  <xsl:apply-templates select="/db:book/db:appendix[@xml:id='changelog']"/>
</xsl:template>

<xsl:template match="db:appendix">
  <xsl:variable name="revision" select="db:revhistory/db:revision[@xml:id=$version]"/>
  <xsl:if test="count($revision) != 1">
    <xsl:message terminate="yes">Cannot find revision {$version} in changelog</xsl:message>
  </xsl:if>
  <xsl:apply-templates select="$revision/db:revdescription"/>
</xsl:template>

<xsl:template match="db:revdescription">
  <body>
    <xsl:apply-templates/>
  </body>
</xsl:template>

<xsl:template match="db:itemizedlist">
  <ul>
    <xsl:apply-templates/>
  </ul>
</xsl:template>

<xsl:template match="db:orderedlist">
  <ol>
    <xsl:apply-templates/>
  </ol>
</xsl:template>

<xsl:template match="db:listitem">
  <li>
    <xsl:apply-templates/>
  </li>
</xsl:template>

<xsl:template match="db:para">
  <p>
    <xsl:apply-templates/>
  </p>
</xsl:template>

<xsl:template match="db:para[empty(node())]" priority="10">
  <p>No significant changes; dependencies updated to the latest version.</p>
</xsl:template>

<xsl:template match="db:link">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="db:emphasis">
  <xsl:text>*</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="db:code|db:literal|db:classname|db:option|db:filename">
  <code>
    <xsl:apply-templates/>
  </code>
</xsl:template>

<xsl:template match="*">
  <xsl:message terminate="yes" select="'Unexpected changelog element: ' || local-name(.)"/>
</xsl:template>

</xsl:stylesheet>
