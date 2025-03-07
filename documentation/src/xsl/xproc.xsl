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
                exclude-result-prefixes="#all"
                expand-text="yes"
                default-mode="m:docbook"
                version="3.0">

<xsl:strip-space elements="p:*"/>

<xsl:variable name="steps-base-uri" select="'https://spec.xproc.org/master/head/steps/'"/>

<xsl:template match="p:declare-step" priority="100">
  <div class="declare-step">

    <xsl:next-match/>

    <xsl:variable name="import" select="../processing-instruction('import')/string()"/>
    <xsl:if test="$import">
      <div>This is an extension step; to use it, your pipeline must include its declaration.
      For example, by including the extension library with an import at the top of your
      pipeline:</div>

      <xsl:variable name="pl" as="element()">
        <screen xmlns="http://docbook.org/ns/docbook"
                        language="xml"
        >&lt;p:import href="https://xmlcalabash.com/ext/library/{$import}"/&gt;</screen>
      </xsl:variable>
      <xsl:apply-templates select="$pl"/>

    </xsl:if>

    <details>
      <summary>Declaration</summary>
      <xsl:variable name="clean-decl">
        <xsl:apply-templates select="." mode="clean-decl"/>
      </xsl:variable>
      <xsl:variable name="pl" as="element()">
        <db:programlisting language="xml">
          <xsl:sequence select="serialize($clean-decl, map {'method':'xml', 'indent':true()})"/>
        </db:programlisting>
      </xsl:variable>
      <xsl:apply-templates select="$pl"/>
    </details>
  </div>
</xsl:template>

<xsl:template match="p:declare-step">
  <xsl:if test="p:input">
    <xsl:variable name="columns" as="xs:integer"
                  select="4
                          + (if (p:input/@select or p:output/@select) then 1 else 0)
                          + (if (p:input/* or p:output/*) then 1 else 0)"/>
    <table class="tableaux">
      <thead>
        <tr>
          <th class="port">Input port</th>
          <th class="check">Primary</th>
          <th class="check">Sequence</th>
          <th>Content types</th>
          <xsl:if test="p:input/@select or p:output/@select">
            <th>Default selection</th>
          </xsl:if>
          <xsl:if test="p:input/* or p:output/*">
            <th class="binding">Default binding</th>
          </xsl:if>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="p:input"/>
      </tbody>
    </table>
  </xsl:if>

  <xsl:if test="p:output">
    <xsl:variable name="columns" as="xs:integer"
                  select="4
                          + (if (p:input/@select or p:output/@select) then 1 else 0)
                          + (if (p:input/* or p:output/*) then 1 else 0)"/>
    <table class="tableaux">
      <thead>
        <tr>
          <th class="port">Output port</th>
          <th class="check">Primary</th>
          <th class="check">Sequence</th>
          <th>Content types</th>
          <xsl:if test="p:input/@select or p:output/@select">
            <th>Default selection</th>
          </xsl:if>
          <xsl:if test="p:input/* or p:output/*">
            <th class="binding">Default binding</th>
          </xsl:if>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="p:output"/>
      </tbody>
    </table>
  </xsl:if>

  <xsl:if test="p:option">
    <xsl:variable name="columns" as="xs:integer"
                  select="2
                          + (if (p:option/@values) then 1 else 0)
                          + (if (p:option/@static) then 1 else 0)
                          + (if (p:option/@required) then 1 else 0)"/>
    <table class="tableaux">
      <thead>
        <tr>
          <th class="optname">Option name</th>
          <th>Type</th>
          <xsl:if test="p:option/@values">
            <th>Values</th>
          </xsl:if>
          <xsl:if test="exists(p:option[not(@required)])">
            <th>Default value</th>
          </xsl:if>
          <xsl:if test="p:option/@static">
            <th>Static</th>
          </xsl:if>
          <xsl:if test="p:option/@required">
            <th class="check">Required</th>
          </xsl:if>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="p:option[@required='true']">
          <xsl:sort select="@name"/>
        </xsl:apply-templates>
        <xsl:apply-templates select="p:option[not(@required='true')]">
          <xsl:sort select="@name"/>
        </xsl:apply-templates>
      </tbody>
    </table>
  </xsl:if>
</xsl:template>

<xsl:template match="p:input|p:output">
  <xsl:if test="exists(@* except (@port|@primary|@sequence|@select|@content-types))">
    <xsl:message terminate="yes" select="'Unexpected attributes:', @* ! node-name(.)"/>
  </xsl:if>

  <xsl:variable name="primary"
                select="@primary = 'true'
                        or (self::p:input and count(../p:input) = 1 and not(@primary='false'))
                        or (self::p:output and count(../p:output) = 1 and not(@primary='false'))"/>

  <tr>
    <td>
      <xsl:variable name="port" select="string(@port)"/>
      <xsl:if test="$primary">
        <xsl:attribute name="class" select="'primary'"/>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="false()"/>
<!--
        <xsl:when test="../../db:variablelist/db:varlistentry/db:term/db:port[. = $port]">
          <a href="#{ancestor::db:section[1]/@xml:id}-def-{$port}">{$port}</a>
        </xsl:when>
        <xsl:when test="../following::db:port[. = $port]">
          <xsl:variable name="first" select="(../following::db:port[. = $port])[1]"/>
          <a href="#port.inline.{$port}-{count($first/preceding::db:port[. = $port])+1}">{$port}</a>
        </xsl:when>
-->
        <xsl:otherwise>
          <xsl:text>{$port}</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td class="check">
      <xsl:if test="$primary">✔</xsl:if>
      <xsl:text> </xsl:text>
    </td>
    <td class="check">
      <xsl:if test="@sequence='true'">✔</xsl:if>
      <xsl:text> </xsl:text>
    </td>
    <td>
      <xsl:text>{@content-types/string()}</xsl:text>
      <xsl:text> </xsl:text>
    </td>
    <xsl:if test="../p:input/@select or ../p:output/@select">
      <td>
        <xsl:text>{@select/string()}</xsl:text>
        <xsl:text> </xsl:text>
      </td>
    </xsl:if>
    <xsl:if test="../p:input/* or ../p:output/*">
      <td>
        <xsl:if test="not(empty(*))">
          <xsl:if test="exists(* except p:empty)">
            <xsl:message terminate="yes"
                         select="'Unexpected default binding:', * ! node-name(.)"/>
          </xsl:if>
          <xsl:text>p:empty</xsl:text>
        </xsl:if>
      </td>
    </xsl:if>
  </tr>
</xsl:template>

<xsl:template match="p:option">
  <xsl:if test="exists(@* except (@name|@as|@values|@select|@required|@static|@e:type))">
    <xsl:message terminate="yes" select="'Unexpected attributes:', @* ! node-name(.)"/>
  </xsl:if>

  <tr>
    <td class="optname">
      <xsl:variable name="name" select="string(@name)"/>
      <xsl:if test="@required='true'">
        <xsl:attribute name="class" select="'required'"/>
      </xsl:if>
      <xsl:text>{$name}</xsl:text>
    </td>
    <td>{(@e:type/string(), @as/string(), "xs:string")[1]}</td>
    <xsl:if test="../p:option/@values">
      <td>
        <xsl:text>{@values/string()}</xsl:text>
        <xsl:text> </xsl:text>
      </td>
    </xsl:if>

    <xsl:if test="exists(../p:option[not(@required)])">
      <td>
        <xsl:choose>
          <xsl:when test="@required"> </xsl:when>
          <xsl:when test="@select">
            <xsl:text>{@select/string()}</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>()</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </xsl:if>

    <xsl:if test="../p:option/@static">
      <td class="check">
        <xsl:if test="@static='true'">✔</xsl:if>
        <xsl:text> </xsl:text>
      </td>
    </xsl:if>
    <xsl:if test="../p:option/@required">
      <td class="check">
        <xsl:if test="@required='true'">✔</xsl:if>
        <xsl:text> </xsl:text>
      </td>
    </xsl:if>
  </tr>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="p:declare-step" mode="clean-decl">
  <xsl:element name="{node-name(.)}" namespace="{namespace-uri(.)}">
    <xsl:apply-templates select="@* except (@xml:id | @xml:base)"/>
    <xsl:apply-templates select="node()" mode="clean-decl"/>
  </xsl:element>
</xsl:template>

<xsl:template match="p:spec-fragments" mode="clean-decl"/>

<xsl:template match="*" mode="clean-decl">
  <xsl:element name="{node-name(.)}" namespace="{namespace-uri(.)}">
    <xsl:apply-templates select="@*,node()" mode="clean-decl"/>
  </xsl:element>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()" mode="clean-decl">
  <xsl:copy/>
</xsl:template>

</xsl:stylesheet>
