<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:array="http://www.w3.org/2005/xpath-functions/array"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:mp="http://docbook.org/ns/docbook/modes/private"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:rddl="http://www.rddl.org/"
                xmlns:t="http://docbook.org/ns/docbook/templates"
                xmlns:tp="http://docbook.org/ns/docbook/templates/private"
                xmlns:v="http://docbook.org/ns/docbook/variables"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                default-mode="m:docbook"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:param name="mediaobject-output-base-uri" select="'./'"/>
<xsl:param name="chunk" select="'index.html'"/>
<xsl:param name="persistent-toc" select="'true'"/>
<xsl:param name="docbook-transclusion" select="'true'"/>
<xsl:param name="revhistory-style" select="'list'"/>

<xsl:template match="*" mode="m:html-head-links">
  <xsl:next-match/>
  <link rel="shortcut icon" href="img/icon.png" />
</xsl:template>

<xsl:variable name="VERSION" select="parse-json(unparsed-text('../../build/version.json'))"/>

<xsl:param name="chunk-exclude"
           select="('self::db:partintro',
                    'self::*[ancestor::db:partintro]',
                    'self::db:section')"/>

<xsl:param name="sections-inherit-from" select="'component section'"/>

<xsl:param name="section-toc-depth" select="1"/>
<xsl:param name="footnote-numeration" select="('*', '**', '†','‡', '§', '1')"/>
<xsl:param name="use-id-as-filename" select="'true'"/>

<xsl:template mode="m:toc-entry"
              match="db:colophon">
  <xsl:param name="persistent" as="xs:boolean" tunnel="yes"/>
  <xsl:param name="root-element" as="element()" tunnel="yes"/>
  <li>
    <a href="#{f:id(.)}">
      <xsl:apply-templates select="." mode="m:headline">
        <xsl:with-param name="purpose" select="'lot'"/>
      </xsl:apply-templates>
    </a>
    <xsl:where-populated>
      <ul class="toc">
        <xsl:apply-templates mode="m:toc-nested">
          <xsl:with-param name="persistent" select="$persistent" tunnel="yes"/>
          <xsl:with-param name="root-element" select="$root-element" tunnel="yes"/>
        </xsl:apply-templates>
      </ul>
    </xsl:where-populated>
  </li>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="db:port">
  <span class="port">
    <xsl:apply-templates/>
  </span>
</xsl:template>

<xsl:template match="db:rfc2119">
  <span class="rfc2119">
    <xsl:apply-templates/>
  </span>
</xsl:template>

<xsl:template match="db:error">
  <span class="pipeline-error">
    <xsl:apply-templates/>
  </span>
</xsl:template>

<xsl:template match="db:error/db:glossterm">
  <span class="glossterm">
    <xsl:next-match/>
    <xsl:text> (</xsl:text>
    <code>
      <xsl:choose>
        <xsl:when test="contains(../@code, ':')">
          <xsl:value-of select="../@code"/>
        </xsl:when>
        <xsl:when test="starts-with(../@code, 'I')">
          <xsl:text>cxerr:X</xsl:text>
          <xsl:value-of select="../@code"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>err:X</xsl:text>
          <xsl:value-of select="../@code"/>
        </xsl:otherwise>
      </xsl:choose>
    </code>
    <xsl:text>)</xsl:text>
  </span>
</xsl:template>

<xsl:template match="db:ghissue">
  <a href="https://github.com/xmlcalabash/xmlcalabash3/issues/{@number}">
    <xsl:text>issue #</xsl:text>
    <xsl:value-of select="@number"/>
  </a>
</xsl:template>

<xsl:template match="processing-instruction('version')">
  <xsl:variable name="ver"
                select="if (normalize-space(.) = '')
                        then 'VERSION'
                        else normalize-space(.)"/>

  <xsl:choose>
    <xsl:when test="map:contains($VERSION, $ver)">
      <xsl:value-of select="map:get($VERSION, $ver)"/>
    </xsl:when>
    <xsl:when test="map:contains($VERSION, upper-case($ver) => replace('-', '_'))">
      <xsl:value-of select="map:get($VERSION, upper-case($ver) => replace('-', '_'))"/>
    </xsl:when>
    <xsl:when test="lower-case($ver) = 'build-date display'">
      <xsl:value-of select="format-date(xs:date(map:get($VERSION, 'BUILD_DATE')), '[D01] [MNn,*-3] [Y0001]')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message select="'Unrecognized version: ' || $ver"/>
      <xsl:value-of select="'UNKNOWN'"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="processing-instruction('dep')" as="xs:string">
  <xsl:variable name="dep" select="normalize-space(.)"/>
  <xsl:variable name="value" select="map:get($VERSION, 'DEPENDENCY_' || $dep)"/>

  <xsl:choose>
    <xsl:when test="empty($value)">
      <xsl:message select="'Unrecognized dependency: ' || $dep"/>
      <xsl:sequence select="'UNRECOGNIZED'"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="string($value)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
