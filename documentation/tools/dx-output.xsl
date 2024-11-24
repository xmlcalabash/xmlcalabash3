<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:deltaxml="http://www.deltaxml.com/ns/well-formed-delta-v1"
                xmlns:dxa="http://www.deltaxml.com/ns/non-namespaced-attribute"
                xmlns:dxx="http://www.deltaxml.com/ns/xml-namespaced-attribute"
                xmlns:ex="https://xmlcalabash.com/ns/examples"
                xmlns:f="https://xmlcalabash.com/functions"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:preserve="http://www.deltaxml.com/ns/preserve"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="#all" 
                expand-text="yes"
                version="3.0">

<xsl:param name="debug" select="'false'"/>

<xsl:output method="xml" encoding="utf-8" indent="no"
            omit-xml-declaration="yes"/>
<xsl:strip-space elements="*"/>

<xsl:variable name="nl" select="'&#10;'"/>
<xsl:variable name="space" as="text()">
  <xsl:text> </xsl:text>
</xsl:variable>

<xsl:template match="/">
  <xsl:variable name="html">
    <html>
      <head>
<style xsl:expand-text="no">
  html { font-size: 14pt; line-height: 1.5; }
</style>
      </head>
      <body>
        <pre><code><xsl:apply-templates/></code></pre>
      </body>
    </html>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$debug != 'false'">
      <xsl:sequence select="$html"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="$html//h:pre" mode="to-docbook"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="*" priority="10000">
  <xsl:choose>
    <xsl:when test="@deltaxml:deltaV2 = 'A'">
      <del><xsl:next-match/></del>
    </xsl:when>
    <xsl:when test="@deltaxml:deltaV2 = 'B'">
      <ins><xsl:next-match/></ins>
    </xsl:when>
    <xsl:otherwise>
      <xsl:next-match/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="*[*]" priority="100">
  <xsl:param name="indent" select="0"/>

  <xsl:value-of select="f:indent($indent)"/>
  <xsl:text>&lt;{node-name(.)}</xsl:text>
  <xsl:sequence select="f:attributes(., $indent)"/>
  <xsl:text>&gt;{$nl}</xsl:text>

  <xsl:apply-templates>
    <xsl:with-param name="indent" select="$indent + 2"/>
  </xsl:apply-templates>

  <xsl:value-of select="f:indent($indent)"/>
  <xsl:text>&lt;/{node-name(.)}&gt;{$nl}</xsl:text>
</xsl:template>

<xsl:template match="*[empty(node())]" priority="100">
  <xsl:param name="indent" select="0"/>

  <xsl:value-of select="f:indent($indent)"/>
  <xsl:text>&lt;{node-name(.)}</xsl:text>
  <xsl:sequence select="f:attributes(., $indent)"/>
  <xsl:text>/&gt;{$nl}</xsl:text>
</xsl:template>

<xsl:template match="*">
  <xsl:param name="indent" select="0"/>

  <xsl:value-of select="f:indent($indent)"/>
  <xsl:text>&lt;{node-name(.)}</xsl:text>
  <xsl:sequence select="f:attributes(., $indent)"/>
  <xsl:text>&gt;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&lt;/{node-name(.)}&gt;{$nl}</xsl:text>
</xsl:template>

<xsl:template match="text()">
  <xsl:copy/>
</xsl:template>

<xsl:template match="comment()">
  <xsl:copy/>
</xsl:template>

<xsl:template match="deltaxml:attributes | preserve:*" priority="1000"/>

<!-- ============================================================ -->

<xsl:template match="attribute()" mode="attributes" as="element()?">
  <attr name="{node-name(.)}" value="{f:quote(string(.))}"/>
</xsl:template>

<xsl:template match="namespace-node()" mode="attributes" as="element()?">
  <xsl:variable name="prefix" select="local-name(.)"/>
  <xsl:variable name="uri" select="string(.)"/>
  <xsl:if test="not(parent::*/ancestor::*/namespace-node()[local-name(.) = $prefix and string(.) = $uri])">
    <xsl:choose>
      <xsl:when test="local-name(.) = ''">
        <attr name="xmlns" value="{f:quote(string(.))}"/>
      </xsl:when>
      <xsl:otherwise>
        <attr name="xmlns:{local-name(.)}" value="{f:quote(string(.))}"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<xsl:template match="namespace-node()[local-name(.) = 'xml'
                       or local-name(.) = 'dxa'
                       or local-name(.) = 'dxx'
                       or local-name(.) = 'preserve'
                       or local-name(.) = 'deltaxml']"
              mode="attributes" priority="100"/>

<xsl:template match="deltaxml:attributes" mode="attributes" as="element()*">
  <xsl:for-each select="dxa:*">
    <xsl:choose>
      <xsl:when test="@deltaxml:deltaV2='A!=B'">
        <attr name="{local-name(.)}"
              valuea="{string(deltaxml:attributeValue[@deltaxml:deltaV2='A'])}"
              valueb="{string(deltaxml:attributeValue[@deltaxml:deltaV2='B'])}"/>
      </xsl:when>
      <xsl:when test="@deltaxml:deltaV2='A'">
        <attr markup="del" name="{local-name(.)}" value="{f:quote(string(.))}"/>
      </xsl:when>
      <xsl:otherwise> <!-- test="@deltaxml:deltaV2='A'" -->
        <attr markup="ins" name="{local-name(.)}" value="{f:quote(string(.))}"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>
</xsl:template>

<xsl:template match="@deltaxml:* | @preserve:*" mode="attributes"/>

<!-- ============================================================ -->

<xsl:template match="*" mode="attribute-markup" as="node()*">
  <xsl:variable name="text" as="node()*">
    <xsl:choose>
      <xsl:when test="@valuea">
        <!-- hack -->
        <xsl:variable name="quoted" select="f:quote(@valuea || @valueb)"/>
        <xsl:text>{@name}={substring($quoted, 1, 1)}</xsl:text>
        <del class="change"><xsl:sequence select="string(@valuea)"/></del>
        <ins class="change"><xsl:sequence select="string(@valueb)"/></ins>
        <xsl:text>{substring($quoted, 1, 1)}</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>{@name}={@value}</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="@markup">
      <xsl:element name="{@markup}" namespace="http://www.w3.org/1999/xhtml">
        <xsl:sequence select="$text"/>
      </xsl:element>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$text"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="f:attributes">
  <xsl:param name="node" as="element()"/>
  <xsl:param name="indent" as="xs:integer"/>

  <xsl:variable name="prefix-length"
                select="$indent + string-length(string(node-name($node))) + 2"/>

  <xsl:variable name="attr" as="element()*">
    <xsl:apply-templates select="$node/namespace::*, $node/@*" mode="attributes"/>
    <xsl:apply-templates select="$node/deltaxml:attributes" mode="attributes"/>
  </xsl:variable>

<!--
  <xsl:message select="node-name($node)"/>
  <xsl:message select="$attr"/>
-->

  <xsl:call-template name="attribute-text">
    <xsl:with-param name="attr" select="$attr"/>
    <xsl:with-param name="prefix-length" select="$prefix-length"/>
    <xsl:with-param name="max-width" select="40"/>
  </xsl:call-template>
</xsl:function>

<xsl:function name="f:indent">
  <xsl:param name="indent" as="xs:integer"/>
  <xsl:for-each select="1 to $indent">
    <xsl:text> </xsl:text>
  </xsl:for-each>
</xsl:function>

<xsl:function name="f:quote" as="xs:string">
  <xsl:param name="value" as="xs:string"/>

  <xsl:choose>
    <xsl:when test="contains($value, '''') and contains($value, '&quot;')">
      <xsl:sequence select="replace($value, '&quot;', '&amp;quot;')"/>
    </xsl:when>
    <xsl:when test="contains($value, '&quot;')">
      <xsl:sequence select="'''' || $value || ''''"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select='"""" || $value || """"'/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<xsl:template name="attribute-text">
  <xsl:param name="attr" as="element()*"/>
  <xsl:param name="prefix-length" as="xs:integer"/>
  <xsl:param name="max-width" as="xs:integer"/>
  <xsl:param name="current-width" as="xs:integer" select="0"/>
  <xsl:param name="current" as="node()*" select="()"/>

  <xsl:variable name="first" as="node()*">
    <xsl:apply-templates select="$attr[1]" mode="attribute-markup"/>
  </xsl:variable>
  <xsl:variable name="first-length" select="string-length(string-join($first, ''))"/>

  <xsl:choose>
    <xsl:when test="empty($attr)">
      <xsl:sequence select="$current"/>
    </xsl:when>
    <xsl:when test="$current-width = 0">
      <xsl:call-template name="attribute-text">
        <xsl:with-param name="attr" select="$attr[position() gt 1]"/>
        <xsl:with-param name="prefix-length" select="$prefix-length"/>
        <xsl:with-param name="max-width" select="$max-width"/>
        <xsl:with-param name="current-width" select="$current-width + $first-length"/>
        <xsl:with-param name="current" select="($space, $first)"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$current-width + $first-length gt $max-width">
      <xsl:sequence select="$current"/>
      <xsl:text>{$nl}</xsl:text>
      <xsl:value-of select="f:indent($prefix-length)"/>
      <xsl:call-template name="attribute-text">
        <xsl:with-param name="attr" select="$attr[position() gt 1]"/>
        <xsl:with-param name="prefix-length" select="$prefix-length"/>
        <xsl:with-param name="max-width" select="$max-width"/>
        <xsl:with-param name="current-width" select="$first-length"/>
        <xsl:with-param name="current" select="$first"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="attribute-text">
        <xsl:with-param name="attr" select="$attr[position() gt 1]"/>
        <xsl:with-param name="prefix-length" select="$prefix-length"/>
        <xsl:with-param name="max-width" select="$max-width"/>
        <xsl:with-param name="current-width" select="$current-width + $first-length"/>
        <xsl:with-param name="current" select="($current, $space, $first)"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="h:pre" mode="to-docbook">
  <xsl:element name="programlisting" namespace="http://docbook.org/ns/docbook">
    <xsl:processing-instruction name="db">verbatim-style="plain"</xsl:processing-instruction>
    <xsl:apply-templates select="h:code/node()" mode="to-docbook"/>
  </xsl:element>
</xsl:template>

<xsl:template match="h:ins" mode="to-docbook">
  <xsl:element name="phrase" namespace="http://docbook.org/ns/docbook">
    <xsl:attribute name="revisionflag" select="'added'"/>
    <xsl:apply-templates mode="to-docbook"/>
  </xsl:element>
</xsl:template>

<xsl:template match="h:ins[contains-token(@class, 'change')]" mode="to-docbook">
  <xsl:element name="phrase" namespace="http://docbook.org/ns/docbook">
    <xsl:attribute name="revisionflag" select="'changed'"/>
    <xsl:apply-templates mode="to-docbook"/>
  </xsl:element>
</xsl:template>

<xsl:template match="h:del" mode="to-docbook">
  <xsl:element name="phrase" namespace="http://docbook.org/ns/docbook">
    <xsl:attribute name="revisionflag" select="'deleted'"/>
    <xsl:apply-templates mode="to-docbook"/>
  </xsl:element>
</xsl:template>

<xsl:template match="h:del[contains-token(@class, 'change')]" mode="to-docbook">
  <!-- suppress -->
</xsl:template>

<xsl:template match="h:*" mode="to-docbook">
  <xsl:message>Unexpected element: {node-name(.)}</xsl:message>
</xsl:template>

<xsl:template match="text()|comment()|processing-instruction()" mode="to-docbook">
  <xsl:copy/>
</xsl:template>



<!-- ============================================================ -->

</xsl:stylesheet>
