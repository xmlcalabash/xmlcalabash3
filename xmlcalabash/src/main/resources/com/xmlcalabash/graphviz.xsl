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

<xsl:variable name="logo" as="xs:string">
R0lGODlhMAAxAPcAAP///wAAAP7+/v39/fz8/Pv7+/r6+vn5+fj4+Pf39/b29vX19fT09PPz8/Ly8vHx
8fDw8O/v7+7u7u3t7ezs7Ovr6+rq6unp6ejo6Ofn5+bm5uXl5eTk5OPj4+Li4uHh4eDg4N/f397e3t3d
3dzc3Nvb29ra2tnZ2djY2NfX19bW1tXV1dTU1NPT09HR0dDQ0M/Pz87Ozs3NzczMzMvLy8rKysnJycjI
yMfHx8bGxsXFxcTExMPDw8LCwsHBwcDAwL+/v76+vr29vbu7u7q6urm5ubi4uLe3t7a2trW1tbS0tLOz
s7KysrGxsbCwsK+vr66urq2traysrKurq6qqqqmpqaioqKenp6ampqWlpaSkpKOjo6GhoaCgoJ+fn56e
np2dnZycnJubm5qampmZmZiYmJeXl5WVlZSUlJKSkpGRkZCQkI+Pj46Ojo2NjYyMjIuLi4qKiomJiYiI
iIaGhoSEhIODg4KCgoGBgX9/f35+fn19fXx8fHt7e3p6enl5eXh4eHd3d3Z2dnV1dXR0dHNzc3JycnFx
cXBwcG9vb25ubm1tbWxsbGtra2pqamlpaWhoaGdnZ2ZmZmVlZWRkZGNjY2JiYmFhYWBgYF9fX15eXlxc
XFtbW1paWllZWVhYWFdXV1ZWVlVVVVRUVFNTU1JSUlFRUVBQUE9PT05OTk1NTUxMTEtLS0pKSklJSUhI
SEdHR0ZGRkVFRURERENDQ0JCQkFBQUBAQD8/Pz4+Pj09PTw8PDs7Ozo6Ojk5OTg4ODc3NzY2NjU1NTQ0
NDMzMzIyMjExMTAwMC8vLy4uLi0tLSwsLCsrKyoqKikpKSgoKCcnJyYmJiUlJSQkJCMjIyIiIiEhISAg
IB8fHx4eHh0dHRwcHBsbGxoaGhgYGBcXFxYWFhUVFRQUFBMTExISEhERERAQEA8PDw4ODg0NDQwMDAsL
CwoKCgkJCQgICAcHBwYGBgUFBQQEBAMDAwICAgEBAf///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
ACH5BAUAAPYALAAAAAAwADEAAAj/AO0JHEiwYEEIM7xYSSFAgA5I0jbJoCBHFKAvJwxq3KhRgSBSIGiE
W4Yp1DIfjM7VenUrVx0yvCY54EizoAMrl2x9AABARzVTtP7wFAZN06IFPAEgQkehZk0Tu4wZ2wWhjDUA
j5gd81EAgA9typYAOOQHwIdzqKAAIeDUoAVpPHvBs4GgCQBbzhYBAFT21zoUAMwFCLWpFq1d3FIJaEvw
kiEVNwQEIJS0wCpqAOZ8AvAnQBmeiaJFYtWqQIFpmxifEOQpkqEf6bDokAUAyqYTKJhRAGAAgBd3ytDw
VCTMmBQAFIJVK+MUhLRqqwapEtLEBwAuEX4EUATAjYikMrjx/zIGiec4UDwLsTP0iUhNM0kIwXOkiwyA
EC4eoDigIwGAPRSEMMoJABBjCiHZ4ACAK0YAgEM6k5RiSioq0CRGFgCk4s0ogABgRgArJLHbA4Eg42EA
GNZhlhMAzNAGT2iYA8kvKJQQyUYChNEMLhAAYI01pNiARTy3DEOIEpgk40ojs+DiySGxdJBUGTYkdUMo
VgDQgi8bYbEKLgHQAsATiOy1zRk1eNJNNIJswYQPG9jzgiXC3CKGColwR8EiBDYgAACV3LIRLC8IUMU1
jvCEQB2dWCAQBjNpVIAMkmCzDgwAlBHAMyvwhEUAfmgkQCxMiNEFAEWgQMEhuDTBmEAzcP/CUy4BnMJT
CMuck4VGKERzSA4b9JaBMdt08epAUexARgCP8IQDOAHAkoBBCZxiyyo0AIDLLa5g88KxAxFASwB28LSF
OerkgoBGPLxghS+SdMDIMDwoAC5BCqjixweTBKCLKOtqpIULcQRQBRLciHGvQQe0oYs61qgRsEYVCLKB
Hh76MsDCG0HQgFMxMALAD5lszDHHBUBwRCBqnMyRBUFkcYcgstBREAKhHGOHI7iY4DJBAuTASjsBwLPM
KbfYTFACceQgRQyghPGzQDd8E8A5acRJkxCGvLFGIiVMbUQAAcRBEwQyDIEDEoy4AQggOuxwgsn3YuFM
AE4QhMAAEUD/McAAa8SBQwQAOBACBxkwwJMDSHgxMWM6JDNZQSMYccQcPUCBhQ0qjADCBDwVIMEKOfxg
hBlgHHtCMc68s0BBBzAgQAMl5IFCBgQkBUABCzAQAQg1UJHIIK8SkEklAShhkAQycNADA0Ec4kUYVeQg
Qgw33JBEE0GI8UgpErzKRh/TWLNYQRYAoYIQGNjzADG6uGIJMfOsQ045AXiDii/TMmaCJWsIQA4MAoAG
bCAEBRgIICTRBU6QLQD0qEc90nGLIhyLE1pghzTORxAG6MAeITDCQJQgCj68IwDrIBvRxjGLDrzqC38o
RABcpREGrGAEMRgIBkLhiQCgIx1XQ0cA/6zxiQkwZgKy2EL+DGAQATTBUSNg4kD6EIp4oGMd8HCHPIY4
igswJhFjYEUAoKAREIChBAogAlsGMoVQPMOHWyRbNkZRgbbQABJXCAA3DiCpjakhCAWRQChKEQB2BKAe
ZBOHKl5XEwKg4gnbSF5NMvCxgpTBE+eARzwO6UNdQMApYmhDZ/a4EQJsjCcGQQEoxuUOTqZjFUakiQQM
8QWy5c0gEIAAAUAAAI4IYhLqIBsi15GKD9QkDFxARQBUsUaCGAAG3wqCCzeCAlbQCh4PfMYMOAIBU6gi
FAHIhs8K8gARICAKRpBDAjlih16kgx7zIBs41sARD3RjFq00FkE2xv8BFtgDBqYIG00eAAxjBCCO8nhG
+zTiiUJkwh3h8IJAILAYCYgBATtYAwwYAwRfGJQeZGOHJhhgEEwIogG9SAY0AOk+e2AABTSQAwnG8EnG
uIEWyyAbPejxjmJIQQMKWAAPapEKEPAiANgQgj0KkIADqKABlHAIHu41CExIg2zzoIc61sGObHCDHucI
RE7dAQeBJEALAICDB54AhKUuDA+r0AYiD/kOdKhDHvNghyGv0YaBCGAHVzCBE0TASI7pYBXeiGM81JEO
drRDgt7YAkEsAAJCPOAAhT3ZAsgAjHXUQx71gCfZkuECghAAAikIwdQKQoAdLCIa3diGN1IRBLoICWQA
zWRMQAAAOw==
</xsl:variable>

<xsl:output method="text" encoding="utf-8"/>

<xsl:strip-space elements="*"/>

<xsl:param name="version" as="xs:string" select="'«unknown»'"/>

<xsl:template match="ns:description">
  <xsl:apply-templates select="." mode="pipelines"/>
  <xsl:apply-templates select="." mode="graphs"/>

  <xsl:result-document href="index.html" method="html" html-version="5" encoding="utf-8" indent="yes">
    <html>
      <head>
        <title>Pipeline visualizations</title>
<style type="text/css" xsl:expand-text="no">
:root {
  background-color: #ffffff;
  font-size: 14pt;
  --symbol-fonts: "Arial Unicode", "Apple Symbols", "Symbol", "Symbola_hint";
  --body-family: serif, var(--symbol-fonts);
  --title-family: sans-serif, var(--symbol-fonts);
  --mono-family: monospace, var(--symbol-fonts);
}
body {
  font-family: var(--body-family);
  max-width: 60rem;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 4rem;
}
header img {
  float: right;
}
h1 {
  font-family: var(--title-family);
}
code {
  font-size: 90%;
}
table {
  layout: fixed;
  width: 100%;
  border: 1px solid #7f7f7f;
  border-spacing: 0px;
  border-collapse: collapse;
}
table thead tr {
  background-color: #cfcfcf;
  font-family: var(--title-family);
}
table thead tr th {
  border-bottom: 1px solid #7f7f7f;
}
table thead th {
  text-align: left;
}
th, td {
  padding: 4px;
  border-right: 1px solid #7f7f7f;
}
table tbody tr:nth-child(even) td {
  background-color: #e7e7e7;
}
a, a:visited {
  text-decoration: none;
}
</style>
      </head>
      <body>
        <header>
          <img src="data:image/gif;base64,{replace($logo, '&#10;', '')}"/>
          <h1>Pipeline visualizations</h1>
        </header>
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
