<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:dot="http://jafpl.com/ns/dot"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"
            omit-xml-declaration="yes"/>

<xsl:param name="debug" select="0"/>

<xsl:strip-space elements="*"/>

<xsl:key name="name" match="*[not(node-name(.) = xs:QName('p:with-option'))]"
         use="@name|@tumble-id"/>
<xsl:key name="id" match="*" use="@xml:id"/>

<xsl:template match="/">
  <dot:digraph name="pg_graph">
    <xsl:if test="$debug = 1">
      <xsl:apply-templates mode="debug-ids"/>
    </xsl:if>
    <xsl:apply-templates/>
    <xsl:apply-templates select="//p:pipe|//p:name-pipe" mode="pipes"/>
  </dot:digraph>
</xsl:template>

<xsl:template match="p:declare-step">
  <xsl:variable name="label"
                select="concat(node-name(.),
                                   if (starts-with(@name, '!'))
                                   then '' else concat(' ', @name))"/>
  <dot:subgraph name="cluster-{generate-id(.)}" label="{$label}" color="black">
    <xsl:call-template name="io"/>
    <xsl:apply-templates/>
  </dot:subgraph>
  <xsl:for-each select="p:output">
    <dot:pipeline-output name="pipeline-output-{@port}"
                         from="{generate-id(..)}-output-{generate-id(.)}"
                         label="{@port}" shape="house"/>
  </xsl:for-each>
  <xsl:for-each select="p:input">
    <dot:pipeline-input name="pipeline-input-{@port}"
                         from="{generate-id(..)}-input-{generate-id(.)}"
                         label="{@port}" shape="invhouse"/>
  </xsl:for-each>
</xsl:template>

<xsl:template match="p:input|p:output|p:with-input|p:with-output|p:pipe|p:name-pipe"/>

<xsl:template match="p:variable|p:option">
  <xsl:variable name="label"
                select="'$' || @name || ' ' || local-name(.)"/>
  <xsl:variable name="expr"
                select="if (string-length(@select) lt 24)
                        then string(@select)
                        else substring(@select, 1, 24) || '…'"/>
  <dot:subgraph name="cluster-{generate-id(.)}" label="{$label}\n{$expr}" color="black">
    <xsl:call-template name="io"/>
    <xsl:apply-templates/>
  </dot:subgraph>
</xsl:template>

<xsl:template match="p:with-option">
<!-- ignore?
  <xsl:variable name="label"
                select="concat(node-name(.),
                                   if (starts-with(@name, '!syn'))
                                   then '' else concat(' ', @name))"/>
  <xsl:if test="*">
    <dot:anchor name="{generate-id(.)}-input-{generate-id(p:with-input)}" label="${@name}"/>
  </xsl:if>
-->
</xsl:template>

<xsl:template match="*">
  <xsl:variable name="step-label" as="xs:string">
    <xsl:choose>
      <xsl:when test="starts-with(@type, 'Q{http://www.w3.org/ns/xproc/}')">
        <xsl:sequence select="'p:' || substring-after(@type, '}')"/>
      </xsl:when>
      <xsl:when test="starts-with(@type, 'Q{http://xmlcalabash.com/ns/extensions/}')">
        <xsl:sequence select="'cx:' || substring-after(@type, '}')"/>
      </xsl:when>
      <xsl:when test="@type">
        <xsl:sequence select="substring-after(@type, '}')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="local-name(.)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="label"
                select="concat(if (starts-with(@name, '!syn'))
                               then '' else concat(@name, ' '),
                               $step-label)"/>
  <dot:subgraph name="cluster-{generate-id(.)}" label="{$label}" color="black">
    <xsl:call-template name="io"/>
    <xsl:apply-templates/>
  </dot:subgraph>
</xsl:template>

<xsl:template match="p:pipe" mode="pipes">
  <xsl:variable name="name" select="@port"/>
  <xsl:variable name="from" select="key('name', @step)"/>
  <xsl:variable name="port" select="$from/p:input[@port=$name]
                                    |$from/p:output[@port=$name]
                                    |$from/p:with-output[@port=$name]"/>
  <xsl:variable name="from-type"
                select="if ($port/self::p:input or $port/self::p:with-input) then 'input' else 'output'"/>
  <xsl:variable name="to-type"
                select="if (parent::p:input or parent::p:with-input) then 'input' else 'output'"/>

  <dot:arrow k="1" from="{generate-id($from)}-{$from-type}-{generate-id($port)}"
             to="{generate-id(../..)}-{$to-type}-{generate-id(..)}"/>
</xsl:template>

<xsl:template match="p:name-pipe" mode="pipes">
  <xsl:variable name="from" select="key('name', @step)"/>
  <dot:arrow from="{$from/@xml:id}-output"
             to="{generate-id(../..)}-binding"/>
</xsl:template>

<xsl:template match="p:name-pipe[ancestor::p:with-option]" mode="pipes">
  <xsl:variable name="from" select="key('name', @step)"/>

  <xsl:choose>
    <xsl:when test="$from/self::p:variable">
      <dot:arrow gid="{generate-id(.)}" from="{$from/@xml:id}-output"
                 to="{generate-id(..)}-input-{generate-id(preceding::p:with-input[1])}"/>
    </xsl:when>
    <xsl:otherwise>
      <dot:arrow gid="{generate-id(.)}" from="{$from/@xml:id}-output"
                 to="{generate-id(../..)}-input-{generate-id(..)}"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:template name="io">
  <xsl:if test="p:input|p:with-input">
    <dot:subgraph name="cluster-{generate-id(.)}-inputs" color="gray" label="inputs" fontcolor="gray" style="rounded">
      <xsl:apply-templates select="p:input|p:with-input" mode="io"/>
    </dot:subgraph>
  </xsl:if>
  <xsl:if test="p:output|p:with-output">
    <dot:subgraph name="cluster-{generate-id(.)}-outputs" color="gray" label="outputs" fontcolor="gray" style="rounded">
      <xsl:apply-templates select="p:output|p:with-output" mode="io"/>
    </dot:subgraph>
  </xsl:if>
  <xsl:if test="p:name-pipe">
    <dot:anchor name="{generate-id(..)}-binding" label="#bindings"/>
  </xsl:if>
</xsl:template>

<xsl:template match="p:input|p:with-input" mode="io">
  <dot:anchor name="{generate-id(..)}-input-{generate-id(.)}" label="{@port}"/>
  <xsl:if test="p:name-pipe">
    <dot:anchor name="{generate-id(..)}-binding" label="#bindings"/>
  </xsl:if>
</xsl:template>

<xsl:template match="p:output|p:with-output" mode="io">
  <dot:anchor name="{generate-id(..)}-output-{generate-id(.)}" label="{@port}"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="element()" mode="debug-ids">
  <xsl:copy>
    <xsl:attribute name="gid" select="generate-id(.)"/>
    <xsl:apply-templates select="@*" mode="debug-ids"/>
    <xsl:apply-templates select="node()" mode="debug-ids"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()"
              mode="debug-ids">
  <xsl:copy/>
</xsl:template>

</xsl:stylesheet>
