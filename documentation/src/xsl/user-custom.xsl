<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:m="http://docbook.org/ns/docbook/modes"
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

<xsl:param name="section-numbers" select="'false'"/>

<xsl:template match="db:book" mode="m:generate-titlepage">
  <header>
    <div class="cover">
      <div class="image">
        <img src="img/xmlcalabash.png" width="400"/>
      </div>
      <div class="text">
        <div class="fill">
          <h1><xsl:value-of select="db:info/db:title"/></h1>
<!--
          <div class="authorgroup">
            <span class="label">
              <xsl:text>Author: </xsl:text>
            </span>
            <xsl:apply-templates select="db:info/db:author"/>
          </div>
-->
          <div class="docversion">
            <xsl:variable name="vid" select="'r' || replace($BUILDINFO/guide-version, '\.', '')"/>
            <xsl:choose>
              <xsl:when test="not(id($vid)) and contains($vid, 'SNAPSHOT')">
                <xsl:text>Version </xsl:text>
                <xsl:value-of select="$BUILDINFO/guide-version"/>
              </xsl:when>
              <xsl:otherwise>
                <a href="#{$vid}">
                  <xsl:text>Version </xsl:text>
                  <xsl:value-of select="$BUILDINFO/guide-version"/>
                </a>
              </xsl:otherwise>
            </xsl:choose>
          </div>
          <div class="version">
            <xsl:text>for XML Calabash </xsl:text>
            <span title="Build id: {$BUILDINFO/build-id}">{$BUILDINFO/version}</span>
          </div>
          <div class="date">
            <xsl:text>Updated: </xsl:text>
            <xsl:apply-templates select="format-date(current-date(), '[D01] [MNn,*-3] [Y0001]')"/>
          </div>
          <!--
          <xsl:if test="$output-media = 'print'">
            <xsl:apply-templates select="db:info/db:revhistory"/>
          </xsl:if>
          -->
          <p class="copyright">
            <a href="copyright{$html-extension}">Copyright</a>
            <xsl:text> &#xA9; </xsl:text>
            <xsl:value-of select="/db:book/db:info/db:copyright/db:year[1]"/>
            <xsl:if test="/db:book/db:info/db:copyright/db:year[2]">
              <xsl:text>–</xsl:text>
              <xsl:value-of select="/db:book/db:info/db:copyright/db:year[last()]"/>
            </xsl:if>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/db:book/db:info/db:copyright/db:holder"/>
            <xsl:text>.</xsl:text>
          </p>
        </div>
      </div>
      <div class="docslink"><a href="https://docs.xmlcalabash.com">docs.xmlcalabash.com</a></div>
    </div>
  </header>
</xsl:template>

<xsl:template match="db:tag">
  <xsl:variable name="formatted" as="element()">
    <xsl:next-match/>
  </xsl:variable>

  <xsl:variable name="tagname" select="normalize-space(string(.))"/>

  <xsl:variable name="link-to" as="xs:string"
                select="if (exists(id(replace($tagname, ':', '.'))))
                        then replace($tagname, ':', '.')
                        else replace($tagname, ':', '-')"/>

  <xsl:variable name="target" as="element()?"
                select="id($link-to)"/>

  <xsl:element name="{node-name($formatted)}"
               namespace="{namespace-uri-from-QName(node-name($formatted))}">
    <xsl:sequence select="$formatted/@*"/>
    <xsl:choose>
      <xsl:when test="@class = 'attribute' or not(contains($tagname, ':')) or @namespace">
        <xsl:sequence select="$formatted/node()"/>
      </xsl:when>
      <xsl:when test="exists($target) and $target = ancestor::*">
        <xsl:sequence select="$formatted/node()"/>
      </xsl:when>

      <xsl:when test="$tagname = ('p:atomic', 'p:catch', 'p:choose', 'p:declare-step', 
                                  'p:document', 'p:documentation', 'p:empty', 'p:extension', 
                                  'p:finally', 'p:for-each', 'p:group', 'p:if', 'p:import', 
                                  'p:import-functions', 'p:inline', 'p:input', 'p:library', 
                                  'p:option', 'p:otherwise', 'p:output', 'p:pipe', 'p:pipeinfo', 
                                  'p:try', 'p:variable', 'p:viewport', 'p:when', 'p:with-input', 
                                  'p:with-option')">
        <a href="https://spec.xproc.org/master/head/xproc/#p.{substring-after($tagname, 'p:')}">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>

      <xsl:when test="$tagname = 'xsd:schema'">
        <a href="https://www.w3.org/TR/xmlschema11-1/#Schemas">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>

      <xsl:when test="$tagname = 's:schema'">
        <a href="https://schematron.com/">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>

      <xsl:when test="$tagname = 'xvrl:report'">
        <a href="https://spec.xproc.org/master/head/xvrl/">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>

      <!-- Speculatively, the rest are steps. -->
      <xsl:when test="starts-with($tagname, 'p:') or starts-with($tagname, 'cx:')">
        <a href="https://docs.xmlcalabash.com/reference/current/{replace($tagname, ':', '-')}">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>

      <xsl:when test="starts-with($tagname, 'c:')">
        <xsl:sequence select="$formatted/node()"/>
      </xsl:when>

      <xsl:when test="starts-with($tagname, 'xsl:')">
        <xsl:sequence select="$formatted/node()"/>
      </xsl:when>

      <xsl:when test="exists($target)">
        <a href="#{$link-to}">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message select="'No target:', $tagname"/>
        <xsl:sequence select="$formatted/node()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>
