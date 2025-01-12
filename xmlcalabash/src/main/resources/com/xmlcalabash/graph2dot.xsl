<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:g="http://xmlcalabash.com/ns/description"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                expand-text="yes"
                exclude-result-prefixes="g p xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"
            omit-xml-declaration="yes"/>

<xsl:param name="number" select="1" as="xs:integer"/>

<xsl:param name="debug" select="0"/>
<xsl:param name="arrows-to-subpipelines" select="'false'"/>

<xsl:key name="id" match="g:atomic|g:head|g:foot|g:sink|g:pipeline|g:subpipeline|g:compound" use="@id"/>
<xsl:key name="sid" match="g:subpipeline" use="@id"/>

<xsl:strip-space elements="*"/>

<xsl:template match="g:description">
  <xsl:variable name="labeled" as="document-node()">
    <xsl:document>
      <xsl:apply-templates select="g:graph[position() = $number]" mode="label"/>
    </xsl:document>
  </xsl:variable>
  <xsl:apply-templates select="$labeled/g:graph"/>
</xsl:template>

<xsl:template match="g:graph">
  <xsl:variable name="inputs" select="g:input"/>
  <xsl:variable name="outputs" select="g:output"/>
  <xsl:variable name="edges" select="g:edge"/>

  <dot:digraph name="pg_graph">
    <!-- Make the graph components separate documents
         in order to scope the keys appropriately. -->
    <xsl:for-each select="g:pipeline|g:compound">
      <xsl:variable name="graph" as="document-node()">
        <xsl:document>
          <xsl:sequence select="."/>
          <xsl:if test="self::g:pipeline">
            <xsl:sequence select="$inputs"/>
            <xsl:sequence select="$outputs"/>
          </xsl:if>
          <xsl:sequence select="$edges"/>
        </xsl:document>
      </xsl:variable>

      <xsl:apply-templates select="$graph/*"/>
    </xsl:for-each>

    <xsl:if test="$arrows-to-subpipelines != 'false'">
      <xsl:for-each select="g:compound">
        <xsl:variable name="compound" select="."/>
        <xsl:variable name="atomic" select="key('sid', @id)"/>

        <xsl:for-each select="$atomic">
          <xsl:variable name="subpipeline" select="."/>

          <!-- Something we can actually point at! -->
          <xsl:variable name="target"
                        select="($compound/*/g:output, $compound/*/g:input)[1]"/>

          <dot:edge from="{$subpipeline/@gid}"
                    output="{$subpipeline/@gid}_name"
                    to="{$target/../@gid}"
                    input="{$target/@gid}"
                    dot:lhead="cluster_{$compound/@gid}"/>
        </xsl:for-each>
      </xsl:for-each>
    </xsl:if>
  </dot:digraph>
</xsl:template>

<xsl:template match="g:pipeline|g:compound">
  <xsl:apply-templates select="g:input"/>

  <dot:subgraph xml:id="cluster_{@gid}" label="{@name}">
    <xsl:apply-templates select="* except (g:input|g:output)"/>
  </dot:subgraph>

  <xsl:apply-templates select="g:output"/>
</xsl:template>

<xsl:template match="g:input|g:output">
  <!-- nop -->
</xsl:template>

<xsl:template match="/g:input">
  <dot:input xml:id="{@gid}" label="{@port}" dot:shape="invhouse"/>

  <xsl:variable name="port" select="@port/string()"/>
  <xsl:variable name="head" select="(/g:pipeline|/g:compound)/g:head"/>

  <dot:edge from="{@gid}"
            input="I_{$head/g:output[@port = $port]/@gid}"
            to="{$head/@gid}"/>
</xsl:template>

<xsl:template match="/g:output">
  <dot:output xml:id="{@gid}" label="{@port}" dot:shape="house"/>

  <xsl:variable name="port" select="@port"/>
  <xsl:variable name="foot" select="(/g:pipeline|/g:compound)/g:foot"/>

  <dot:edge from="{$foot/@gid}"
            output="O_{$foot/g:input[@port = $port]/@gid}"
            to="{@gid}"/>
</xsl:template>

<xsl:template match="g:head|g:foot">
  <dot:subgraph xml:id="{@gid}">
    <xsl:call-template name="generate-table">
      <xsl:with-param name="label" select="local-name(.)"/>
    </xsl:call-template>
  </dot:subgraph>
</xsl:template>

<xsl:template match="g:sink">
  <!-- don't make a node if we aren't going to point to it -->
  <xsl:variable name="this" select="."/>

  <xsl:variable name="edges" select="//g:edge[@to = $this/@id]"/>
  <xsl:if test="count($edges) gt 1">
    <xsl:message select="'Not exactly one edge to sink: ' || $this/@id || '?'"/>
  </xsl:if>

  <xsl:for-each select="$edges">
    <xsl:variable name="edge" select="."/>
    <xsl:if test="not(starts-with($edge/@from, '!foot'))">
      <dot:node xml:id="{$this/@gid}" dot:shape="point"/>
    </xsl:if>
  </xsl:for-each>
</xsl:template>

<xsl:template match="g:atomic[@tag='cx:sink']" priority="10">
  <!-- If the sink was added by the graph builder, don't pretend it's a real node. -->
  <dot:node xml:id="{@gid}" dot:shape="point"/>
</xsl:template>

<xsl:template match="g:atomic|g:subpipeline">
  <dot:subgraph xml:id="{@gid}">
    <xsl:call-template name="generate-table">
      <xsl:with-param name="label" select="@name"/>
    </xsl:call-template>
  </dot:subgraph>
</xsl:template>

<xsl:template name="generate-table">
  <xsl:param name="label" as="xs:string"/>

  <xsl:variable name="head_inputs" as="element(g:output)*">
    <xsl:if test="self::g:head">
      <xsl:sequence select="if (../@tag = ('p:for-each', 'p:viewport'))
                            then g:output[@port ne 'current']
                            else if (../tag = 'p:choose')
                                 then g:output[@port ne '!context']
                                 else g:output"/>
    </xsl:if>
  </xsl:variable>

  <xsl:variable name="inputs"
                select="g:input, $head_inputs"/>

  <xsl:variable name="outputs"
                select="g:output, self::g:foot/g:input"/>

  <xsl:variable name="icount" select="count($inputs)"/>
  <xsl:variable name="ocount" select="count($outputs)"/>

  <xsl:variable name="total-span"
                select="if ($icount gt 0 and $ocount gt 0)
                        then $icount * $ocount
                        else max(($icount, $ocount, 1))"/>

  <xsl:variable name="ispan"
                select="if ($icount gt 0 and $ocount gt 0)
                        then ($icount * $ocount) div $icount
                        else max(($ocount, 1))"/>

  <xsl:variable name="ospan"
                select="if ($icount gt 0 and $ocount gt 0)
                        then ($icount * $ocount) div $ocount
                        else max(($icount, 1))"/>

  <table cellspacing="0" border="0" cellborder="1">
    <xsl:if test="$inputs">
      <tr>
        <xsl:for-each select="$inputs">
          <td port="{(if (self::g:output) then 'I_' else '') || @gid}">
            <xsl:if test="$ispan ne 1">
              <xsl:attribute name="colspan" select="$ispan"/>
            </xsl:if>
            <xsl:variable name="welded" select="@welded-shut = 'true'"/>
            <xsl:variable name="lines" select="tokenize(@port, '/')"/>
            <xsl:for-each select="$lines">
              <xsl:if test="position() gt 1"><br/></xsl:if>
              <xsl:sequence select=". || (if ($welded) then ' ⊗' else '')"/>
            </xsl:for-each>
          </td>
        </xsl:for-each>
      </tr>
    </xsl:if>

    <tr>
      <td port="{@gid}_name">
        <xsl:if test="$total-span ne 1">
          <xsl:attribute name="colspan" select="$total-span"/>
        </xsl:if>
        <xsl:if test="@tag">
          <xsl:text>{string(@tag)}</xsl:text>
          <br/>
        </xsl:if>
        <xsl:if test="true() or self::g:subpipeline or not(starts-with($label, '!'))">
          <xsl:text>{@id}</xsl:text>
        </xsl:if>
      </td>
    </tr>

    <xsl:if test="$outputs">
      <tr>
        <xsl:for-each select="$outputs">
          <td port="{(if (self::g:input) then 'O_' else '') || @gid}">
            <xsl:if test="$ospan ne 1">
              <xsl:attribute name="colspan" select="$ospan"/>
            </xsl:if>
            <xsl:variable name="lines" select="tokenize(@port, '/')"/>
            <xsl:for-each select="$lines">
              <xsl:if test="position() gt 1"><br/></xsl:if>
              <xsl:sequence select="."/>
            </xsl:for-each>
          </td>
        </xsl:for-each>
      </tr>
    </xsl:if>
  </table>
</xsl:template>

<xsl:template match="g:edge">
  <xsl:variable name="fromStep"
                select="(key('id', @from), key('id', @from)/g:head)[last()]"/>
  <xsl:variable name="fromPort" select="string(@output)"/>
  <xsl:variable name="toStep" select="key('id', @to)"/>
  <xsl:variable name="toPort" select="string((@input, 'source')[1])"/>

  <xsl:variable name="from" select="$fromStep/g:output[@port = $fromPort]"/>
  <xsl:variable name="to" select="$toStep/g:input[@port = $toPort]"/>

  <xsl:if test="$from and $to">
    <xsl:variable name="style"
                  select="if (contains(@input, '!depends')
                              or contains(@output, '!depends'))
                          then 'dashed'
                          else 'solid'"/>

    <dot:edge from="{$fromStep/@gid}" output="{$from/@gid}"
              to="{$toStep/@gid}">
      <!-- p:sink edges don't have an @input -->
      <xsl:if test="@input">
        <xsl:attribute name="input" select="$to/@gid"/>
      </xsl:if>
      <xsl:if test="$style ne 'solid'">
        <xsl:attribute name="dot:style" select="$style"/>
      </xsl:if>
    </dot:edge>
  </xsl:if>
</xsl:template>

<xsl:template match="g:*">
  <xsl:message select="'Unexpected: ' || node-name(.)"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:mode name="label" on-no-match="shallow-copy"/>
<xsl:template match="*" mode="label">
  <xsl:element name="{node-name(.)}" namespace="{namespace-uri(.)}">
    <xsl:apply-templates select="@*" mode="label"/>
    <xsl:attribute name="gid" select="generate-id(.)"/>
    <xsl:apply-templates select="node()" mode="label"/>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>
