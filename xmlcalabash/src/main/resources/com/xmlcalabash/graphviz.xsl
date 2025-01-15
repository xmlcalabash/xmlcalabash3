<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                xmlns:ns="http://xmlcalabash.com/ns/description"
                xmlns:f="http://xmlcalabash.com/ns/functions"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                expand-text="yes"
                version="3.0">

<xsl:import href="pipelines.xsl"/>
<xsl:import href="graphs.xsl"/>
<xsl:import href="dot-to-text.xsl"/>

<xsl:output method="text" encoding="utf-8"/>

<xsl:strip-space elements="*"/>

<xsl:param name="version" as="xs:string" select="'«unknown»'"/>

<xsl:template match="ns:description">
  <xsl:apply-templates select="." mode="pipelines"/>
  <xsl:apply-templates select="." mode="graphs"/>

  <xsl:result-document href="index.html" method="html" html-version="5" encoding="utf-8" indent="yes">
    <html>
      <head>
        <title>Pipeline graphs</title>
      </head>
      <body>
        <h1>Pipeline graphs</h1>
        <p>
          <xsl:text>XML Calabash version {$version} on </xsl:text>
          <xsl:value-of select='format-dateTime(current-dateTime(),
			        "[D01] [MNn,*-3] [Y0001] at [h01]:[m01]:[s01]")'/>
        </p>

        <xsl:variable name="trim-prefix"
                      select="f:trim-prefix(distinct-values(ns:declare-step/@base-uri/string())) || '/'"/>

        <p>Base URI: <code>{$trim-prefix}</code></p>

        <table>
          <thead>
            <tr><th>XProc pipeline document</th><th>Pipeline</th><th>Graph</th></tr>
          </thead>
          <tbody>
            <xsl:apply-templates select="ns:declare-step[1]">
              <xsl:with-param name="trim-prefix" select="$trim-prefix"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="ns:declare-step[position() gt 1]">
              <xsl:sort select="@type"/>
              <xsl:with-param name="trim-prefix" select="$trim-prefix"/>
            </xsl:apply-templates>
          </tbody>
        </table>
      </body>
    </html>
  </xsl:result-document>

  <xsl:for-each select="ns:declare-step">
    <xsl:text>pipelines/{@id}&#10;</xsl:text>
  </xsl:for-each>

  <xsl:for-each select="ns:graph/ns:pipeline">
    <xsl:text>graphs/{@id}&#10;</xsl:text>
  </xsl:for-each>
</xsl:template>

<xsl:template match="ns:declare-step">
  <xsl:param name="trim-prefix" as="xs:string"/>
  <tr>
    <td>
      <code><xsl:value-of select="substring-after(@base-uri, $trim-prefix)"/></code>
    </td>
    <td>
      <a href="pipelines/{@id}.svg">{(@type, @id)[1]/string()}</a>
    </td>
    <td>
      <xsl:variable name="graph" select="(following-sibling::ns:graph)[1]"/>
      <a href="graphs/{$graph/ns:pipeline/@id}.svg">{$graph/ns:pipeline/@id/string()}</a>
    </td>
  </tr>
</xsl:template>

<xsl:function name="f:trim-prefix" as="xs:string">
  <xsl:param name="files" as="xs:string*"/>
  <xsl:choose>
    <xsl:when test="count($files) = 1">
      <xsl:sequence select="string-join(tokenize($files, '/')[position() lt last()], '/')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="f:trim-prefix($files, '')"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<xsl:function name="f:trim-prefix" as="xs:string">
  <xsl:param name="files" as="xs:string*"/>
  <xsl:param name="prefix" as="xs:string"/>
  
  <xsl:variable name="firsts"
                select="$files ! tokenize(., '/')[1]"/>

  <xsl:choose>
    <xsl:when test="empty($firsts)">
      <xsl:sequence select="$prefix"/>
    </xsl:when>
    <xsl:when test="count(distinct-values($firsts)) = 1">
      <xsl:variable name="rests"
                    select="$files ! string-join(tokenize(., '/')[position() gt 1], '/')"/>
      <xsl:variable name="new-prefix"
                    select="if ($prefix = '') then $firsts[1] else $prefix || '/' || $firsts[1]"/>
      <xsl:sequence select="f:trim-prefix($rests, $new-prefix)"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$prefix"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

</xsl:stylesheet>
