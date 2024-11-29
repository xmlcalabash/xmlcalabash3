<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dot="http://jafpl.com/ns/dot"
                xmlns:ns="http://xmlcalabash.com/ns/description"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                expand-text="yes"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"
            omit-xml-declaration="yes"/>

<xsl:param name="number" select="1" as="xs:integer"/>

<xsl:param name="debug" select="0"/>
<xsl:param name="arrows-to-subpipelines" select="'false'"/>

<xsl:key name="step" match="ns:atomic-step|ns:compound-step|ns:declare-step" use="@name"/>

<xsl:strip-space elements="*"/>

<xsl:variable name="nl" select="'&#10;'"/>

<xsl:template match="ns:description">
  <xsl:variable name="pipeline" as="document-node()">
    <xsl:document>
      <xsl:sequence select="ns:declare-step[position() = $number]"/>
    </xsl:document>
  </xsl:variable>
  <xsl:apply-templates select="$pipeline"/>
</xsl:template>

<xsl:template match="ns:declare-step|ns:library">
  <dot:digraph name="pg_pipeline">
    <xsl:apply-templates select="ns:input"/>

    <xsl:call-template name="compound-step"/>

    <xsl:apply-templates select="ns:output"/>

    <xsl:apply-templates select="//ns:pipe"/>

    <xsl:for-each select="ns:input">
      <dot:edge x="1" from="{generate-id(.)}"
                to="cluster_{generate-id(..)}_head"
                input="{generate-id(.)}_head_input"/>
    </xsl:for-each>

    <xsl:for-each select="ns:output">
      <dot:edge x="2" to="{generate-id(.)}"
                from="cluster_{generate-id(..)}_foot"
                output="{generate-id(.)}_foot"/>
    </xsl:for-each>
  </dot:digraph>
</xsl:template>

<xsl:template match="ns:compound-step" name="compound-step">
  <dot:subgraph xml:id="cluster_{generate-id(.)}" labeljust="c">
    <xsl:attribute name="label">
      <xsl:sequence select="@type"/>
      <xsl:if test="not(starts-with(@name, '!'))">
        <xsl:sequence select="' / ' || @name"/>
      </xsl:if>
    </xsl:attribute>

    <xsl:if test="ns:with-input|ns:input">
      <dot:subgraph xml:id="cluster_{generate-id(.)}_head" peripheries="0" shape="diamond">
        <xsl:call-template name="compound-head"/>
      </dot:subgraph>
    </xsl:if>
    <xsl:apply-templates select="ns:atomic-step|ns:compound-step"/>
    <dot:subgraph xml:id="cluster_{generate-id(.)}_foot" peripheries="0" shape="diamond">
      <xsl:call-template name="compound-foot"/>
    </dot:subgraph>
  </dot:subgraph>
</xsl:template>

<xsl:template name="compound-head">
  <xsl:variable name="input"
                select="if (self::ns:declare-step)
                        then (ns:with-input|ns:input)
                        else ns:with-input"/>

  <xsl:variable name="icount" select="count($input)"/>
  <xsl:variable name="ocount" select="count(ns:input)"/>

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
    <xsl:if test="$input">
      <tr>
        <xsl:for-each select="$input">
          <td port="{generate-id(.)}_head_input">
            <xsl:if test="$ispan ne 1">
              <xsl:attribute name="colspan" select="$ispan"/>
            </xsl:if>
            <xsl:text>{string(@port)}{if (@welded-shut) then " ⊗ " else ""}</xsl:text>
          </td>
        </xsl:for-each>
      </tr>
    </xsl:if>
    <xsl:if test="ns:input">
      <tr>
        <xsl:for-each select="ns:input">
          <td port="{generate-id(.)}_head_output">
            <xsl:if test="$ospan ne 1">
              <xsl:attribute name="colspan" select="$ospan"/>
            </xsl:if>
            <xsl:text>{string(@port)}{if (@welded-shut) then " ⊗ " else ""}</xsl:text>
          </td>
        </xsl:for-each>
      </tr>
    </xsl:if>
  </table>
</xsl:template>

<xsl:template name="compound-foot">
  <xsl:variable name="icount" select="count(ns:output)"/>
  <xsl:variable name="ocount" select="count(ns:output)"/>

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
    <xsl:if test="ns:output">
      <tr>
        <xsl:for-each select="ns:output">
          <td port="{generate-id(.)}_foot">
            <xsl:if test="$ispan ne 1">
              <xsl:attribute name="colspan" select="$ispan"/>
            </xsl:if>
            <xsl:text>{string(@port)}</xsl:text>
          </td>
        </xsl:for-each>
      </tr>
    </xsl:if>
<!--
    <xsl:if test="ns:output">
      <tr>
        <xsl:for-each select="ns:output">
          <td port="{generate-id(.)}_foot_output">
            <xsl:if test="$ospan ne 1">
              <xsl:attribute name="colspan" select="$ospan"/>
            </xsl:if>
            <xsl:text>{string(@port)}</xsl:text>
          </td>
        </xsl:for-each>
      </tr>
    </xsl:if>
-->
  </table>
</xsl:template>

<xsl:template match="ns:atomic-step">
  <xsl:variable name="icount" select="count(ns:with-input)"/>
  <xsl:variable name="ocount" select="count(ns:with-output)"/>

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

  <dot:subgraph xml:id="cluster_{generate-id(.)}" peripheries="0">
    <table cellspacing="0" border="0" cellborder="1">
      <xsl:if test="ns:with-input">
        <tr>
          <xsl:for-each select="ns:with-input">
            <td port="{generate-id(.)}">
              <xsl:if test="$ispan ne 1">
                <xsl:attribute name="colspan" select="$ispan"/>
              </xsl:if>
              <xsl:text>{string(@port)}{if (@welded-shut) then " ⊗ " else ""}</xsl:text>
            </td>
          </xsl:for-each>
        </tr>
      </xsl:if>
      <tr>
        <td>
          <xsl:if test="$total-span ne 1">
            <xsl:attribute name="colspan" select="$total-span"/>
          </xsl:if>
          <xsl:text>{@type/string()}</xsl:text>
          <xsl:if test="not(starts-with(@name, '!'))">
            <xsl:text> / {@name/string()}</xsl:text>
          </xsl:if>
        </td>
      </tr>
      <xsl:if test="@expression">
        <tr>
          <td>
            <xsl:if test="$total-span ne 1">
              <xsl:attribute name="colspan" select="$total-span"/>
            </xsl:if>
            <i>{@expression/string()}</i>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="@select">
        <tr>
          <td>
            <xsl:if test="$total-span ne 1">
              <xsl:attribute name="colspan" select="$total-span"/>
            </xsl:if>
            <i>{@select/string()}</i>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="@href">
        <tr>
          <td>
            <xsl:if test="$total-span ne 1">
              <xsl:attribute name="colspan" select="$total-span"/>
            </xsl:if>
            <xsl:text>href = "{@href}"</xsl:text>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="ns:with-output">
        <tr>
          <xsl:for-each select="ns:with-output">
            <td port="{generate-id(.)}">
              <xsl:if test="$ospan ne 1">
                <xsl:attribute name="colspan" select="$ospan"/>
              </xsl:if>
              <xsl:text>{string(@port)}</xsl:text>
            </td>
          </xsl:for-each>
        </tr>
      </xsl:if>
    </table>
    <xsl:apply-templates select="ns:atomic-step|ns:compound-step"/>
  </dot:subgraph>
</xsl:template>

<xsl:template match="ns:input">
  <dot:input xml:id="{generate-id(.)}" dot:shape="invhouse">
    <xsl:attribute name="label">
      <xsl:text>{@port}{if (@welded-shut) then " ⊗ " else ""}</xsl:text>
    </xsl:attribute>
  </dot:input>
</xsl:template>

<xsl:template match="ns:output">
  <dot:output xml:id="{generate-id(.)}" label="{@port}" dot:shape="house"/>
</xsl:template>

<xsl:template match="ns:atomic-step/ns:with-input/ns:pipe
                     |ns:with-option/ns:with-input/ns:pipe">
  <xsl:variable name="this" select="."/>
  <xsl:variable name="to_step" select="../.."/>
  <xsl:variable name="to_port" select=".."/>
  <xsl:variable name="from_step" select="key('step', @step)"/>
  <xsl:variable name="from_port" select="$from_step/*[@port = $this/@port]"/>

<!--
  <xsl:message select="'1.', node-name($from_step), $from_port/@port/string(), '→',
                       node-name($to_step), $to_port/@port/string()"/>
-->

  <xsl:choose>
    <xsl:when test="($from_step/self::ns:compound-step or $from_step/self::ns:declare-step)
                    and $from_port/self::ns:input">
<!--
      <xsl:message>cluster_{generate-id($from_step)}_head . {generate-id($from_port)}_head_output → cluster_{generate-id($to_step)} . {generate-id($to_port)}</xsl:message>
-->

<!--
<xsl:message select="'STEP:', @step/string()"/>
<xsl:message select="'FROM:', $from_step, ' :: ', $from_port"/>
<xsl:message select="'TO:', $to_step, ' :: ', $to_port"/>
-->

      <dot:edge x="3" to="cluster_{generate-id($to_step)}" input="{generate-id($to_port)}"
                from="cluster_{generate-id($from_step)}_head" output="{generate-id($from_port)}_head_output"/>
    </xsl:when>
    <xsl:when test="($from_step/self::ns:compound-step or $from_step/self::ns:declare-step)
                    and $from_port/self::ns:output">
<!--
      <xsl:message>cluster_{generate-id($from_step)}_head . {generate-id($from_port)}_head_output → cluster_{generate-id($to_step)} . {generate-id($to_port)}</xsl:message>
-->

      <dot:edge x="4" to="cluster_{generate-id($to_step)}" input="{generate-id($to_port)}"
                from="cluster_{generate-id($from_step)}_foot" output="{generate-id($from_port)}_foot"/>
    </xsl:when>
    <xsl:otherwise>

    <!--
      <xsl:message select="'FROM-STEP:', $from_step"/>
      <xsl:message select="'FROM-PORT:', $from_port"/>
      <xsl:message select="'TO-STEP:', $to_step"/>
      <xsl:message select="'TO-STEP:', $to_port"/>
      <xsl:message>cluster_{generate-id($from_step)} . {generate-id($from_port)} → cluster_{generate-id($to_step)} . {generate-id($to_port)}</xsl:message>
    -->

      <xsl:for-each select="$from_port">
        <dot:edge x="5" to="cluster_{generate-id($to_step)}" input="{generate-id($to_port)}"
                  from="cluster_{generate-id($from_step)}" output="{generate-id(.)}"/>
      </xsl:for-each>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="ns:compound-step/ns:with-input/ns:pipe">
  <xsl:variable name="this" select="."/>
  <xsl:variable name="to_step" select="../.."/>
  <xsl:variable name="to_port" select=".."/>
  <xsl:variable name="from_step" select="key('step', @step)"/>
  <xsl:variable name="from_port" select="$from_step/*[@port = $this/@port]"/>

<!--
  <xsl:message select="'2.', node-name($from_step), $from_port/@port/string(), '→',
                       node-name($to_step), $to_port/@port/string()"/>
-->

  <xsl:choose>
    <xsl:when test="($from_step/self::ns:compound-step or $from_step/self::ns:declare-step)
                    and $from_port/self::ns:input">
<!--
      <xsl:message>cluster_{generate-id($from_step)}_head . {generate-id($from_port)}_head_output → cluster_{generate-id($to_step)}_head . {generate-id($to_port)}_head_input</xsl:message>
-->
      <dot:edge x="6" to="cluster_{generate-id($to_step)}_head" input="{generate-id($to_port)}_head_input"
                from="cluster_{generate-id($from_step)}_head" output="{generate-id($from_port)}_head_output"/>
    </xsl:when>
    <xsl:otherwise>
<!--
      <xsl:message>cluster_{generate-id($from_step)}. {generate-id($from_port)} → cluster_{generate-id($to_step)}_head . {generate-id($to_port)}_head_input</xsl:message>
-->
      <dot:edge x="7" to="cluster_{generate-id($to_step)}_head" input="{generate-id($to_port)}_head_input"
                from="cluster_{generate-id($from_step)}" output="{generate-id($from_port)}"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="ns:compound-step/ns:output/ns:pipe
                     |ns:declare-step/ns:output/ns:pipe">
  <xsl:variable name="this" select="."/>
  <xsl:variable name="from_step" select="key('step', @step)"/>
  <xsl:variable name="from_port" select="$from_step/*[@port = $this/@port]"/>
  <xsl:variable name="to_step" select="../.."/>
  <xsl:variable name="to_port" select=".."/>

<!--
  <xsl:message select="'FROM:', @step/string(), @port/string(), count($from_step), count($from_port)"/>
  <xsl:message select="'TO:', count($to_step), count($to_port)"/>

  <xsl:message select="'3.', node-name($from_step), $from_port/@port/string(), '→',
                       node-name($to_step), $to_port/@port/string()"/>
-->

  <xsl:choose>
    <xsl:when test="$from_step/self::ns:compound-step and $from_port/self::ns:output">
      <dot:edge x="8" to="cluster_{generate-id($to_step)}_foot" input="{generate-id($to_port)}_foot"
                from="cluster_{generate-id($from_step)}_foot" output="{generate-id($from_port)}_foot"/>
    </xsl:when>
    <xsl:otherwise>
<!--
      <xsl:message select="$this"/>
      <xsl:message select="'cluster_' || generate-id($from_step)"/>
      <xsl:message select="$from_step"/>
      <xsl:message select="'cluster_' || generate-id($to_step) || '_foot'"/>
      <xsl:message select="$to_step"/>
-->
      <dot:edge x="9" to="cluster_{generate-id($to_step)}_foot" input="{generate-id($to_port)}_foot"
                from="cluster_{generate-id($from_step)}" output="{generate-id($from_port)}"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="ns:*">
  <xsl:message select="'Unexpected: ' || node-name(.)"/>
</xsl:template>

</xsl:stylesheet>
