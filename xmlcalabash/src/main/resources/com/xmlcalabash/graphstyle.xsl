<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                xmlns:g="http://xmlcalabash.com/ns/description"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="xs"
                expand-text="yes"
                version="3.0">
<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:key name="port" match="g:port" use="@id"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="g:declare-step">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:attribute name="dot:label"
                   select="if (starts-with(@name, '!'))
                           then 'p:declare-step'
                           else 'p:declare-step “' || @name || '”'"/>

    <xsl:apply-templates select="node()"/>
    
    <!-- There's a little bit of graph modification here. Not all
         outputs have explicit sinks in the actual graph, but the SVG
         graph looks nice if they're uniformly connected. -->
    <xsl:call-template name="add-sinks"/>
    <xsl:for-each select="//g:head/g:outputs/g:port">
      <xsl:variable name="id" select="@id"/>
      <xsl:if test="empty(//g:edge[@from=$id])">
        <g:edge from="{$id}" to="sink_{generate-id(.)}"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:copy>
</xsl:template>

<xsl:template match="g:compound-step">
  <xsl:variable name="label"
                select="if (starts-with(@name, '!'))
                        then @type || g:step-number(root(), @type, @id)
                        else @type || g:step-number(root(), @type, @id) || ' “' || @name || '”'"/>

  <xsl:message select="string(@type), string(@name), string(@id), '::', $label"/>
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:attribute name="dot:label" select="$label"/>
    <xsl:apply-templates select="node()"/>
    <xsl:call-template name="add-sinks"/>
  </xsl:copy>
</xsl:template>

<xsl:template name="add-sinks">
    <xsl:for-each select="g:head/g:outputs/g:port">
      <xsl:variable name="id" select="@id"/>
      <xsl:if test="empty(//g:edge[@from=$id])">
        <g:node xml:id="sink_{generate-id(.)}" dot:shape="point"/>
      </xsl:if>
    </xsl:for-each>
</xsl:template>

<xsl:template match="g:input">
  <g:input dot:peripheries="0" dot:shape="invhouse"
           h:cellspacing="0" h:border="0" h:cellborder="1">
    <xsl:apply-templates select="@*,node()"/>
  </g:input>
</xsl:template>

<xsl:template match="g:output">
  <g:input dot:peripheries="0" dot:shape="house"
           h:cellspacing="0" h:border="0" h:cellborder="1">
    <xsl:apply-templates select="@*,node()"/>
  </g:input>
</xsl:template>

<!-- Give primary ports a light-gray background -->
<!--
<xsl:template match="g:port[@primary='true']">
  <xsl:copy>
    <xsl:attribute name="h:bgcolor" select="'#dadada'"/>
    <xsl:apply-templates select="@*,node()"/>
  </xsl:copy>
</xsl:template>
-->

<xsl:template match="g:head|g:foot">
  <xsl:copy>
    <xsl:attribute name="dot:shape" select="'parallelogram'"/>
    <xsl:attribute name="dot:peripheries" select="0"/>
    <xsl:attribute name="h:cellspacing" select="0"/>
    <xsl:attribute name="h:border" select="0"/>
    <xsl:attribute name="h:cellborder" select="1"/>
    <xsl:apply-templates select="@*,node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="g:atomic-step|g:subpipeline">
  <g:atomic-step dot:peripheries="0" h:cellspacing="0" h:border="0" h:cellborder="1">
    <xsl:apply-templates select="@*"/>

    <xsl:apply-templates select="g:inputs"/>

    <xsl:choose>
      <xsl:when test="@type='cx:expression'">
        <xsl:choose>
          <xsl:when test="@variable-name">
            <g:detail>variable</g:detail>
            <g:detail>${string(@variable-name)}</g:detail>
          </xsl:when>
          <xsl:when test="@option-name">
            <g:detail>option</g:detail>
            <g:detail>${string(@option-name)}</g:detail>
          </xsl:when>
          <xsl:otherwise>
            <g:detail>cx:expression</g:detail>
          </xsl:otherwise>
        </xsl:choose>
        
        <xsl:choose>
          <xsl:when test="not(@expression)"/>
          <xsl:when test="string-length(@expression) lt 20">
            <g:detail><i>{string(@expression)} </i></g:detail>
          </xsl:when>
          <xsl:otherwise>
            <g:detail><i>{substring(@expression, 1, 20)}… </i></g:detail>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <g:detail>
          <td>
            <xsl:choose>
              <xsl:when test="self::g:subpipeline">
                <xsl:attribute name="dot:href" select="'#cluster_' || replace(@ref, '-', '_')"/>
                <font dot:color="#0000FF">
                  <xsl:text>{@type}</xsl:text>
                  <xsl:sequence select="g:step-number(root(), @type, @ref)"/>
                </font>
              </xsl:when>
              <xsl:when test="@filename">
                <xsl:attribute name="dot:href" select="@filename || '.html'"/>
                <font dot:color="#0000FF">
                  <xsl:text>{@type}</xsl:text>
                </font>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>{@type}</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </td>
          <xsl:if test="not(starts-with(@name, '!'))">
            <td>“{string(@name)}”</td>
          </xsl:if>
        </g:detail>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates select="g:outputs"/>
  </g:atomic-step>
</xsl:template>

<xsl:template match="g:atomic-step[@type='cx:sink']">
  <g:node xml:id="{g:inputs/g:port/@id}" dot:shape="point"/>
</xsl:template>

<xsl:template match="g:edge">
  <xsl:variable name="style" as="attribute()?">
    <xsl:choose>
      <xsl:when test="starts-with(@from-port, '!depends') or starts-with(@to-port, '!depends')">
        <xsl:attribute name="dot:style" select="'dashed'"/>
      </xsl:when>
      <!-- Make "implicit" connections gray -->
      <!--
      <xsl:when test="@implicit = 'true'">
        <xsl:attribute name="dot:color" select="'#aaaaaa'"/>
      </xsl:when>
      -->
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="from-port" select="key('port', @from)"/>
  <xsl:variable name="to-port" select="key('port', @to)"/>

  <xsl:variable name="head" as="attribute()?">
    <xsl:attribute name="dot:arrowhead" select="'normal'"/>
  </xsl:variable>

  <xsl:variable name="tail" as="attribute()*">
  </xsl:variable>

  <g:edge>
    <xsl:if test="$from-port/@sequence='true' and $to-port/@sequence='false'">
      <xsl:attribute name="dot:label" select="'⋮'"/>
    </xsl:if>
    <xsl:sequence select="($style, $head, $tail)"/>
    <xsl:apply-templates select="@*, node()"/>
  </g:edge>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="g:step-number" as="xs:string?">
  <xsl:param name="graph" as="document-node()"/>
  <xsl:param name="type" as="xs:string"/>
  <xsl:param name="id" as="xs:string?"/>

  <xsl:if test="exists($id) and count($graph//g:compound-step[@type=$type]) gt 1">
    <xsl:variable name="step" select="$graph//g:compound-step[@type=$type and @id=$id]"/>
    <xsl:variable name="num" select="count($step/preceding::g:compound-step[@type=$type])+1"/>
    <xsl:sequence select="' (' || $num || ')'"/>
  </xsl:if>
</xsl:function>

</xsl:stylesheet>
