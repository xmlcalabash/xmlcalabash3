<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:md="https://xmlcalabash.com/ns/markdown-extensions"
                xmlns:mp="http://docbook.org/ns/docbook/modes/private"
                xmlns:p="http://www.w3.org/ns/xproc"
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

<xsl:template match="md:extensions">
  <p>The following extensions are based on <em>Flexmark-java</em> version {@version/string()}.</p>

  <table>
    <thead>
      <tr>
        <th>Extension name</th>
        <th>Flexmark-java extension</th>
      </tr>
    </thead>
    <tbody>
      <xsl:for-each select="md:extension">
        <tr>
          <td><a href="#md-ext.{@name}">{@name/string()}</a></td>
          <td><code title="{@package}.{@class}">{@class/string()}</code></td>
        </tr>
      </xsl:for-each>
    </tbody>
  </table>

  <xsl:apply-templates select="md:extension"/>

</xsl:template>

<xsl:template match="md:extension">
  <div class="extension" id="md-ext.{@name}">
    <h2>{@name/string()} extension</h2>

    <p>Consult documentation for the <a href="https://github.com/vsch/flexmark-java">Flexmark-java</a>
    extension <code>{@package/string()}.{@class/string()}</code>.</p>

    <xsl:choose>
      <xsl:when test="empty(md:option)">
        <p>This extension has no configurable options.</p>
      </xsl:when>
      <xsl:otherwise>
        <p>This extension has the following configurable options:</p>
        <table class="md-ext-opt">
          <thead>
            <tr>
              <th class="md-ext-prop">Property</th>
              <th class="md-ext-value">Value</th>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates select="md:option"/>
          </tbody>
        </table>
      </xsl:otherwise>
    </xsl:choose>
  </div>
</xsl:template>

<xsl:template match="md:option">
  <tr>
    <td><code>{md:name(@enum)}</code></td>
    <td>
      <xsl:choose>
        <xsl:when test="@class">
          <xsl:for-each select="tokenize(@values, '\s+')">
            <code><xsl:sequence select="md:name(.)"/></code>
            <xsl:if test="position() lt last()"> | </xsl:if>
          </xsl:for-each>
        </xsl:when>
        <xsl:when test="@type='integer'">
          <em>xs:integer</em>
        </xsl:when>
        <xsl:when test="@type='boolean'">
          <em>xs:boolean</em>
        </xsl:when>
        <xsl:when test="not(@type) or @type='string'">
          <em>xs:string</em>
        </xsl:when>
        <xsl:when test="@type='DataKeyMap:string,string'">
          <em>map(xs:string, xs:string)</em>
        </xsl:when>
        <xsl:when test="@type='DataKeyArray:string'">
          <em>xs:string*</em>
        </xsl:when>
        <xsl:when test="@type='Map:character,integer' or @type='DataKeyMap:character,integer'">
          <em>map(xs:string, xs:integer)</em> (String must be a single character.)
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>???</xsl:text>
          <xsl:message>Unmatched: {@type/string()}</xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </td>
  </tr>
</xsl:template>

<xsl:function name="md:name" as="xs:string">
  <xsl:param name="enum" as="xs:string"/>
  <xsl:sequence select="lower-case($enum) => replace('_', '-')"/>
</xsl:function>

</xsl:stylesheet>
