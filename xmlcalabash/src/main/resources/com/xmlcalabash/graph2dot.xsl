<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                xmlns:g="http://xmlcalabash.com/ns/description"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                expand-text="yes"
                version="3.0">

<xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:param name="debug" select="'0'"/>

<xsl:template match="g:pipeline-container | g:graph-container">
  <dot:digraph name="pipeline">
    <xsl:copy-of select="@dot:*"/>
    <xsl:apply-templates/>
  </dot:digraph>
</xsl:template>

<xsl:template match="g:graph">
  <xsl:apply-templates select="g:input"/>
  <dot:subgraph src="{node-name(.)}" xml:id="cluster_{generate-id(.)}">
    <xsl:copy-of select="@dot:*"/>
    <xsl:apply-templates select="* except (g:head|g:tail|g:input|g:output|g:edge)"/>
  </dot:subgraph>
  <xsl:apply-templates select="g:output"/>
  <xsl:apply-templates select="//g:edge"/>
</xsl:template>

<xsl:template match="g:pipeline-container/g:declare-step">
  <xsl:apply-templates select="g:input"/>
  <dot:subgraph src="{node-name(.)}" xml:id="cluster_{generate-id(.)}" dot:style="solid">
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates select="g:head"/>
    <xsl:apply-templates select="* except (g:head|g:tail|g:input|g:output|g:edge)"/>
    <xsl:apply-templates select="g:tail"/>
  </dot:subgraph>
  <xsl:apply-templates select="g:output"/>
  <xsl:apply-templates select="//g:edge"/>
</xsl:template>

<xsl:template match="g:atomic-step|g:subpipeline">
  <dot:subgraph src="{node-name(.)}" xml:id="{g:cluster-id(.)}" dot:style="solid">
    <xsl:apply-templates select="@* except @h:*"/>
    <table>
      <xsl:apply-templates select="@h:*"/>
      <xsl:apply-templates select="g:inputs"/>
      <xsl:apply-templates select="g:detail"/>
      <xsl:apply-templates select="g:outputs"/>
    </table>
  </dot:subgraph>
</xsl:template>

<xsl:template match="g:compound-step|g:graph-container//g:declare-step">
  <dot:subgraph src="{node-name(.)}" xml:id="{g:cluster-id(.)}" dot:style="solid">
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates select="g:head"/>
    <xsl:apply-templates select="* except (g:head|g:tail|g:edge)"/>
    <xsl:apply-templates select="g:tail"/>
  </dot:subgraph>
</xsl:template>

<xsl:template match="g:head|g:foot">
  <xsl:where-populated>
    <dot:subgraph src="{node-name(.)}" xml:id="{g:cluster-id(.)}" dot:style="solid">
      <xsl:apply-templates select="@* except @h:*"/>
      <xsl:where-populated>
        <table>
          <xsl:apply-templates select="@h:*"/>
          <xsl:apply-templates select="g:inputs"/>
          <xsl:apply-templates select="g:outputs"/>
        </table>
      </xsl:where-populated>
    </dot:subgraph>
  </xsl:where-populated>
</xsl:template>

<xsl:template match="g:input|g:output">
  <dot:subgraph src="{node-name(.)}" xml:id="{g:cluster-id(.)}" dot:style="solid">
    <xsl:apply-templates select="@* except @h:*"/>
    <table>
      <xsl:apply-templates select="@h:*"/>
      <tr>
        <xsl:apply-templates select="g:port"/>
      </tr>
    </table>
  </dot:subgraph>
</xsl:template>

<xsl:template match="g:inputs|g:outputs">
  <xsl:if test="g:port">
    <xsl:variable name="widths"
                  select="( ../g:inputs[g:port] ! count(g:port),
                            ../g:outputs[g:port] ! count(g:port),
                            ../g:detail[h:td] ! count(h:td),
                            ../g:detail[not(*)] ! 1
                          )"/>
    <xsl:variable name="product" select="fold-left($widths, 1, function($a,$b) { $a * $b })"/>
    <xsl:variable name="span" select="$product idiv count(g:port)"/>
    <tr>
      <xsl:apply-templates>
        <xsl:with-param name="span" select="$span"/>
      </xsl:apply-templates>
    </tr>
  </xsl:if>
</xsl:template>

<xsl:template match="g:port">
  <xsl:param name="span" select="1" as="xs:integer"/>
  <td port="{@id}">
    <xsl:if test="$span gt 1">
      <xsl:attribute name="colspan" select="$span"/>
    </xsl:if>
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates/>
  </td>
</xsl:template>

<xsl:template match="g:port/text()[starts-with(., 'Q{')]">
  <xsl:variable name="ns" select="substring(substring-before(., '}'), 3)"/>
  <xsl:variable name="local-name" select="substring-after(., '}')"/>
  <xsl:choose>
    <xsl:when test="$ns = ''">
      <xsl:sequence select="'$' || $local-name"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="prefixes" as="xs:string*">
        <xsl:for-each select="../namespace::*">
          <xsl:if test=". = $ns">
            <xsl:sequence select="local-name(.)"/>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="empty($prefixes)">
          <xsl:sequence select="."/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="'$' || $prefixes[1] || ':' || $local-name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="g:detail">
  <xsl:variable name="widths"
                select="( ../g:inputs[g:port] ! count(g:port),
                          ../g:outputs[g:port] ! count(g:port),
                          ../g:detail[h:td] ! count(h:td),
                          ../g:detail[not(*)] ! 1
                        )"/>
  <xsl:variable name="product" select="fold-left($widths, 1, function($a,$b) { $a * $b })"/>
  <xsl:variable name="span"
                select="if (*) then $product idiv count(*) else $product"/>
  <tr>
    <xsl:choose>
      <xsl:when test="h:td">
        <xsl:for-each select="h:td">
          <td>
            <xsl:if test="$span gt 1">
              <xsl:attribute name="colspan" select="$span"/>
            </xsl:if>
            <xsl:apply-templates select="@*,node()" mode="gviz-html"/>
          </td>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <td>
          <xsl:if test="$span gt 1">
            <xsl:attribute name="colspan" select="$span"/>
          </xsl:if>
          <xsl:apply-templates select="@*,node()" mode="gviz-html"/>
        </td>
      </xsl:otherwise>
    </xsl:choose>
  </tr>
</xsl:template>

<xsl:template match="g:node">
  <dot:node>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates select="node()"/>
  </dot:node>
</xsl:template>

<xsl:template match="g:edge">
  <dot:edge>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates select="node()"/>
  </dot:edge>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="*" mode="gviz-html">
  <xsl:element name="{local-name(.)}">
    <xsl:apply-templates select="@*, node()" mode="gviz-html"/>
  </xsl:element>
</xsl:template>

<xsl:template match="text()|attribute()" mode="gviz-html">
  <xsl:copy/>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="@*"/>

<xsl:template match="@dot:*|@h:*">
  <xsl:attribute name="{local-name(.)}" select="string(.)"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="g:cluster-id" as="xs:string">
  <xsl:param name="node" as="element()"/>
  <xsl:choose>
    <xsl:when test="$node/@id">
      <xsl:sequence select="'cluster_' || replace($node/@id, '-', '_')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="'cluster_' || generate-id($node)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

</xsl:stylesheet>
