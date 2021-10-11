<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                exclude-result-prefixes="map xs"
                version="3.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:param name="previous" required="true"/>
<xsl:variable name="prun" select="document($previous)"/>

<xsl:template match="/">
  <xsl:variable name="fail" as="map(*)">
    <xsl:map>
      <xsl:for-each select="//testcase[failure]/@name/string()">
        <xsl:map-entry key="." select="1"/>
      </xsl:for-each>
    </xsl:map>
  </xsl:variable>
  <xsl:variable name="pass" as="map(*)">
    <xsl:map>
      <xsl:for-each select="//testcase[empty(failure)]/@name/string()">
        <xsl:map-entry key="." select="1"/>
      </xsl:for-each>
    </xsl:map>
  </xsl:variable>

  <xsl:variable name="pfail" as="map(*)">
    <xsl:map>
      <xsl:for-each select="$prun//testcase[failure]/@name/string()">
        <xsl:map-entry key="." select="1"/>
      </xsl:for-each>
    </xsl:map>
  </xsl:variable>
  <xsl:variable name="ppass" as="map(*)">
    <xsl:map>
      <xsl:for-each select="$prun//testcase[empty(failure)]/@name/string()">
        <xsl:map-entry key="." select="1"/>
      </xsl:for-each>
    </xsl:map>
  </xsl:variable>

  <xsl:variable name="new-fail" as="xs:string*">
    <xsl:for-each select="map:keys($fail)">
      <xsl:if test="not(map:contains($pfail, .))">
        <xsl:value-of select="."/>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="new-pass" as="xs:string*">
    <xsl:for-each select="map:keys($pass)">
      <xsl:if test="not(map:contains($ppass, .))">
        <xsl:value-of select="."/>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:text expand-text="yes">Changes since the {$prun/testsuite/@timestamp} report:
{xs:integer(testsuite/@tests) - xs:integer($prun/testsuite/@tests)} new tests.
New passing tests: {count($new-pass)}
New failing tests: {count($new-fail)}
</xsl:text>

  <xsl:text expand-text="yes">Newly passing ({count($new-pass)}): </xsl:text>
  <xsl:choose>
    <xsl:when test="exists($new-pass)">
      <xsl:sequence select="string-join($new-pass, ', ')"/>
    </xsl:when>
    <xsl:otherwise>None</xsl:otherwise>
  </xsl:choose>
  <xsl:text>&#10;</xsl:text>

  <xsl:text expand-text="yes">Newly failing ({count($new-fail)}): </xsl:text>
  <xsl:choose>
    <xsl:when test="exists($new-fail)">
      <xsl:sequence select="string-join($new-fail, ', ')"/>
    </xsl:when>
    <xsl:otherwise>None</xsl:otherwise>
  </xsl:choose>
  <xsl:text>&#10;</xsl:text>

  <xsl:if test="exists(map:keys($fail))">
    <xsl:variable name="list"
                  select="if (count(map:keys($fail)) > 5)
                          then (subsequence(map:keys($fail), 1, 4), '…')
                          else map:keys($fail)"/>
    <xsl:text expand-text="yes"
      >Failing ({count(map:keys($fail))}): {string-join($list, ', ')}</xsl:text>
    <xsl:text>&#10;</xsl:text>
  </xsl:if>

  <xsl:text>Failing tests:&#10;</xsl:text>
  <xsl:for-each select="map:keys($fail)">
    <xsl:sequence select=". || '&#10;'"/>
  </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
