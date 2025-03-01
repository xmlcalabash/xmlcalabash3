<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                xmlns:ns="http://xmlcalabash.com/ns/description"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                expand-text="yes"
                default-mode="pipelines"
                version="3.0">

<xsl:strip-space elements="*"/>

<xsl:param name="debug" select="'0'"/>

<xsl:key name="step" match="ns:atomic-step|ns:compound-step|ns:declare-step" use="@name"/>

<xsl:variable name="nl" select="'&#10;'"/>

<xsl:variable name="typed-declare-steps" select="//ns:declare-step[@type]"/>

<xsl:template match="ns:description">
  <xsl:for-each select="ns:declare-step">
    <xsl:variable name="pipeline" as="document-node()">
      <xsl:document>
        <xsl:sequence select="."/>
      </xsl:document>
    </xsl:variable>
    <xsl:variable name="dotxml">
      <xsl:apply-templates select="$pipeline"/>
    </xsl:variable>

    <xsl:if test="$debug != '0'">
      <xsl:result-document href="pipelines/{@id}.xml" method="xml" indent="yes">
        <xsl:sequence select="$dotxml"/>
      </xsl:result-document>
    </xsl:if>

    <xsl:result-document href="pipelines/{@id}.dot" method="text">
      <xsl:apply-templates select="$dotxml" mode="dot-to-text"/>
    </xsl:result-document>
  </xsl:for-each>
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
    <xsl:variable name="step-name" as="xs:string?"
                  select="if (not(starts-with(@name, '!')))
                          then string(@name)
                          else ()"/>

    <xsl:attribute name="label"
                   select="@type
                           || (if (@type and $step-name) then ' / ' else ())
                           || $step-name"/>

    <xsl:if test="ns:with-input|ns:input">
      <dot:subgraph xml:id="cluster_{generate-id(.)}_head" peripheries="0" shape="diamond">
        <xsl:call-template name="compound-head"/>
      </dot:subgraph>
    </xsl:if>

    <xsl:apply-templates select="ns:atomic-step|ns:compound-step"/>

    <xsl:if test="ns:output">
      <dot:subgraph xml:id="cluster_{generate-id(.)}_foot" peripheries="0" shape="diamond">
        <xsl:call-template name="compound-foot"/>
      </dot:subgraph>
    </xsl:if>
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

<xsl:template match="ns:atomic-step[@type='cx:sink']">
  <dot:node xml:id="{generate-id(.)}" dot:shape="point"/>
</xsl:template>

<xsl:template match="ns:atomic-step">
  <xsl:variable name="icount" select="count(ns:with-input)"/>
  <xsl:variable name="ocount" select="count(ns:with-output)"/>
  <xsl:variable name="tag" select="@type"/>
  <xsl:variable name="pipeline" select="$typed-declare-steps[@type = $tag]"/>

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
          <xsl:if test="$pipeline">
            <xsl:attribute name="href" select="$pipeline/@id || '.svg'"/>
          </xsl:if>

          <xsl:variable name="display-name" as="xs:string"
                        select="if (@option-name)
                                then '$' || @option-name
                                else @type"/>

          <xsl:choose>
            <xsl:when test="$pipeline">
              <font color="#0000ff">
                <xsl:text>{$display-name}</xsl:text>
              </font>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>{$display-name}</xsl:text>
            </xsl:otherwise>
          </xsl:choose>

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

            <xsl:choose>
              <xsl:when test="contains(@expression, 'Q{http://xmlcalabash.com/ns/extensions}')
                              and contains(@expression, 'static')
                              and contains(@expression, '18,')">
                <i>«required»</i>
              </xsl:when>
              <xsl:when test="string-length(@expression) gt 17">
                <i>{substring(@expression, 1, 17)}…</i>
              </xsl:when>
              <xsl:otherwise>
                <i>{@expression/string()}</i>
              </xsl:otherwise>
            </xsl:choose>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="@select">
        <tr>
          <td>
            <xsl:if test="$total-span ne 1">
              <xsl:attribute name="colspan" select="$total-span"/>
            </xsl:if>

            <xsl:choose>
              <xsl:when test="string-length(@select) gt 17">
                <i>{substring(@select, 1, 17)}…</i>
              </xsl:when>
              <xsl:otherwise>
                <i>{@select/string()}</i>
              </xsl:otherwise>
            </xsl:choose>
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


<xsl:message select="'STEP:', @step/string()"/>
<xsl:message select="'FROM:', $from_step, ' :: ', $from_port"/>
<xsl:message select="'TO:', $to_step, ' :: ', $to_port"/>
<xsl:message select="count($to_step), count($to_port), count($from_step), count($from_port)"/>
-->

  <dot:edge x="3" to="cluster_{generate-id($to_step)}" input="{generate-id($to_port)}"
            from="cluster_{generate-id($from_step)}_head" output="{generate-id($from_port)}_head_output"/>

    </xsl:when>
    <xsl:when test="($from_step/self::ns:compound-step or $from_step/self::ns:declare-step)
                    and $from_port/self::ns:output">

      <xsl:variable name="to_label" as="xs:string">
        <xsl:choose>
          <xsl:when test="$to_step/@type = 'cx:sink'">
            <xsl:sequence select="generate-id($to_step)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:sequence select="'cluster_' || generate-id($to_step)"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

<!--
      <xsl:message>cluster_{generate-id($from_step)}_head . {generate-id($from_port)}_head_output → {$to_label} . {generate-id($to_port)}</xsl:message>
-->

      <dot:edge x="4" to="{$to_label}" input="{generate-id($to_port)}"
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
        <xsl:choose>
          <xsl:when test="$to_step/@type = 'cx:sink'">
            <dot:edge x="5a" to="{generate-id($to_step)}"
                      from="cluster_{generate-id($from_step)}" output="{generate-id(.)}"/>
          </xsl:when>
          <xsl:otherwise>
            <dot:edge x="5b" to="cluster_{generate-id($to_step)}" input="{generate-id($to_port)}"
                      from="cluster_{generate-id($from_step)}" output="{generate-id(.)}"/>
          </xsl:otherwise>
        </xsl:choose>
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

      <dot:edge x="7" 
                to="cluster_{generate-id($to_step)}_head" input="{generate-id($to_port)}_head_input"
                output="{generate-id($from_port)}">
        <xsl:attribute name="from">
          <xsl:choose>
            <xsl:when test="$from_port/parent::ns:compound-step">
              <xsl:sequence select="'cluster_' || generate-id($from_step) || '_foot'"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:sequence select="'cluster_' || generate-id($from_step)"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </dot:edge>
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
    <xsl:when test="$from_port/self::ns:input">
      <dot:edge x="10" to="cluster_{generate-id($to_step)}_foot" input="{generate-id($to_port)}_foot"
                from="cluster_{generate-id($from_step)}_head" output="{generate-id($from_port)}_head_input"/>
    </xsl:when>

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
