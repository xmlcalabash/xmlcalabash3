<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dot="http://xmlcalabash.com/ns/dot"
                xmlns:ns="http://xmlcalabash.com/ns/description"
                xmlns:f="http://xmlcalabash.com/ns/functions"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                expand-text="yes"
                version="3.0">

<xsl:import href="https://xmlcalabash.com/xsl/static.xsl"/>

<xsl:output method="html" html-version="5" encoding="utf-8"/>

<xsl:param name="version" as="xs:string" select="'«unknown»'"/>

<xsl:template match="ns:description">
  <html>
    <head>
      <title>Pipeline visualizations</title>
      <xsl:sequence select="$css"/>
      </head>
      <body>
        <header>
          <h1><img class="__logo" src="data:image/gif;base64,{replace($logo, '&#10;', '')}"
          />Pipeline visualizations</h1>
        </header>
        <p>
          <xsl:text>XML Calabash version {$version} on </xsl:text>
          <xsl:value-of select='format-dateTime(current-dateTime(),
			        "[D01] [MNn,*-3] [Y0001] at [H01]:[m01]:[s01]")'/>
        </p>

        <xsl:variable name="trim-prefix"
                      select="f:trim-prefix(distinct-values(ns:declare-step/@base-uri/string())) || '/'"/>

        <p>Base URI: <code>{$trim-prefix}</code></p>

        <table>
          <thead>
            <tr><th>XProc pipeline document</th><th>Declare step</th><th>Graph</th></tr>
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
</xsl:template>

<xsl:template match="ns:declare-step">
  <xsl:param name="trim-prefix" as="xs:string"/>
  <tr>
    <td>
      <code><xsl:value-of select="substring-after(@base-uri, $trim-prefix)"/></code>
    </td>
    <td>
      <a href="pipelines/{@filename}.html">{string((@type, @filename)[1])}</a>
    </td>
    <td>
      <a href="graphs/{@filename}.html">{string((@type, @filename)[1])}</a>
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
