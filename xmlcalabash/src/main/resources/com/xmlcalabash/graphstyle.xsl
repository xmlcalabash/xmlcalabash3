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
<xsl:import href="https://xmlcalabash.com/xsl/static.xsl"/>

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:key name="port" match="g:port" use="@id"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="/g:declare-step">
  <g:pipeline-container>
    <xsl:next-match/>
  </g:pipeline-container>
</xsl:template>

<xsl:template match="/g:graph">
  <g:graph-container>
    <xsl:next-match/>
  </g:graph-container>
</xsl:template>

<xsl:template match="g:declare-step">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:call-template name="container-style"/>

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

<xsl:template match="g:graph">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:attribute name="dot:style" select="'invis'"/>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<xsl:template match="g:compound-step">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:call-template name="container-style"/>
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

<xsl:template match="g:head|g:foot">
  <xsl:copy>
    <xsl:attribute name="dot:peripheries" select="0"/>
    <xsl:attribute name="h:cellspacing" select="0"/>
    <xsl:attribute name="h:border" select="0"/>
    <xsl:attribute name="h:cellborder" select="1"/>
    <xsl:apply-templates select="@*,node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="g:atomic-step|g:subpipeline">
  <g:atomic-step dot:peripheries="0"
                 h:cellspacing="0"
                 h:border="0"
                 h:cellborder="1">
    <xsl:apply-templates select="@*"/>

    <xsl:apply-templates select="g:inputs"/>

    <xsl:choose>
      <xsl:when test="@type='cx:expression'">
        <xsl:choose>
          <xsl:when test="@variable-name">
            <g:detail><td>variable</td></g:detail>
            <g:detail><td bgcolor="{$light-orange}">${string(@variable-name)}</td></g:detail>
          </xsl:when>
          <xsl:when test="@option-name">
            <g:detail><td>option</td></g:detail>
            <g:detail><td bgcolor="{$light-orange}">${string(@option-name)}</td></g:detail>
          </xsl:when>
          <xsl:otherwise>
            <g:detail><td>cx:expression</td></g:detail>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:variable name="width" select="24"/>
        <xsl:choose>
          <xsl:when test="not(@expression)"/>
          <xsl:when test="string-length(@expression) lt $width">
            <g:detail>
              <td>
                <font face="{$expression-font}"
                      point-size="10"
                      >{string(@expression)}</font>
              </td>
            </g:detail>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="first"
                          select="substring(@expression, 1, $width idiv 2)"/>
            <xsl:variable name="last"
                          select="substring(@expression,
                                     string-length(@expression) - ($width idiv 2))"/>
            <g:detail>
              <td>
                <font face="{$expression-font}"
                      point-size="10"
                      >{$first} ⋯ {$last}</font>
              </td>
            </g:detail>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>

      <xsl:otherwise>
        <g:detail>
          <td>
            <xsl:if test="not(starts-with(@type, 'cx:'))">
              <xsl:attribute name="bgcolor" select="$light-orange"/>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="self::g:subpipeline">
                <xsl:attribute name="href" select="'#cluster_' || replace(@ref, '-', '_')"/>
                <font color="#0000FF">
                  <xsl:text>{@type}</xsl:text>
                  <xsl:sequence select="g:step-number(root(), @type, @ref)"/>
                </font>
              </xsl:when>
              <xsl:when test="@filename">
                <xsl:attribute name="href" select="@filename || '.html'"/>
                <font color="#0000FF">
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
        <xsl:if test="@href">
          <g:detail>
            <td>href="{string(@href)}"</td>
          </g:detail>
        </xsl:if>
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

<xsl:template name="container-style">
  <xsl:variable name="label" as="xs:string">
    <xsl:choose>
      <xsl:when test="self::g:declare-step">
        <xsl:sequence
            select="if (starts-with(@name, '!'))
                    then 'p:declare-step'
                    else 'p:declare-step “' || @name || '”'"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence 
            select="if (starts-with(@name, '!'))
                    then @type || g:step-number(root(), @type, @id)
                    else @type || g:step-number(root(), @type, @id)
                               || ' “' || @name || '”'"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

    <xsl:attribute name="dot:label" select="$label"/>
    <xsl:attribute name="dot:fontname" select="$label-font"/>
    <xsl:attribute name="dot:style" select="'rounded'"/>
    <xsl:attribute name="dot:color" select="$sky-blue"/>
    <xsl:attribute name="dot:penwidth" select="2"/>
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
