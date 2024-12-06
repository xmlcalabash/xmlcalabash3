<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:math="http://www.w3.org/2005/xpath-functions/math"
                xmlns:f="https://xmlcalabash.com/ns/functions"
                xmlns:m="https://xmlcalabash.com/ns/modes"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="html" html-version="5" encoding="utf-8" indent="yes"/>

<xsl:template match="/">
  <html>
    <head>
      <title>XML Calabash Test Report</title>
      <link rel="stylesheet" href="test-report.css"/>
      <script defer="defer" src="filter.js"/>
    </head>
    <body>
      <xsl:variable name="extra-xml" select="resolve-uri('extra-suite-results.xml', base-uri(.))"/>
      <xsl:variable name="extra"
                    select="if (doc-available($extra-xml)) then doc($extra-xml)/testsuite else ()"/>

      <h1>XML Calabash Test Report</h1>
      
      <xsl:if test="exists($extra)">
        <ul>
          <li><a href="#test-suite">XProc test suite</a></li>
          <li><a href="#extra-suite">XML Calabash test suite</a></li>
        </ul>
      </xsl:if>

      <div id="test-suite">
        <xsl:apply-templates>
          <xsl:with-param name="link" select="true()" tunnel="yes"/>
        </xsl:apply-templates>
      </div>
      <div id="extra-suite">
        <xsl:apply-templates select="$extra"/>
      </div>      
    </body>
  </html>
</xsl:template>

<xsl:template match="testsuite">
  <div class="suite">
    <h1>{@name/string()}</h1>
    <div class="detail">
      <xsl:variable name="dt" select="xs:dateTime(@timestamp)"/>
      <xsl:variable name="dtf" select="'[D01] [MNn,*-3] [Y0001] at [H01]:[m01]'"/>

      <xsl:variable name="total" select="xs:integer(@tests)"/>
      <xsl:variable name="fail" select="xs:integer(@errors)"/>
      <xsl:variable name="skip" select="xs:integer(@skipped)"/>
      <xsl:variable name="pass" select="$total - ($fail + $skip)"/>

      <p>
        <xsl:text>The suite has {format-number($total, "#,###")} tests; </xsl:text>
        <xsl:text>{format-number($pass, "#,###")} passed, </xsl:text>
        <xsl:text>{format-number($skip, "#,###")} skipped, </xsl:text>
        <xsl:text>{format-number($fail, "#,###")} failed.</xsl:text>
      </p>
      <p>
        <xsl:text>Ran {format-number($total - $skip, "#,###")} tests in </xsl:text>
        <xsl:variable name="seconds" select="xs:double(@time)"/>
        <xsl:choose>
          <xsl:when test="$seconds gt 60">
            <xsl:variable name="minutes" select="floor($seconds div 60.0)"/>
            <xsl:text>{$minutes}m</xsl:text>
            <xsl:text>{format-number($seconds - (60*$minutes), '#.##')}s</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>{format-number($seconds, '#.##')}s</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text> on {format-dateTime($dt, $dtf)}Z: </xsl:text>
        <xsl:text>{format-number(100.0 * $pass div ($total - $skip), '#.##')}% pass.</xsl:text>
      </p>
    </div>

    <xsl:apply-templates select="properties"/>

    <table>
      <colgroup>
        <col style="width:2em;"/>
        <col style="width:16em;"/>
        <col style="width:5em;"/>
        <col style="width:5em;"/>
      </colgroup>
      <thead>
        <tr>
          <td>#</td>
          <td>Test case</td>
          <td>Status</td>
          <td>Time (s)</td>
          <td>Comments</td>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="testcase">
          <xsl:sort select="@name"/>
        </xsl:apply-templates>
      </tbody>
    </table>
  </div>
</xsl:template>

<xsl:template match="properties">
  <p class="properties">
    <xsl:text>{property[@name="processor"]/@value/string()} </xsl:text>
    <xsl:text>version {property[@name="version"]/@value/string()} </xsl:text>
    <xsl:text>(build {property[@name="gitHash"]/@value/string()}); </xsl:text>
    <xsl:text>Saxon </xsl:text>
    <xsl:text>version {property[@name="saxonVersion"]/@value/string()}; </xsl:text>
    <xsl:text>XProc </xsl:text>
    <xsl:text>version {property[@name="xprocVersion"]/@value/string()}; </xsl:text>
    <xsl:text>XPath </xsl:text>
    <xsl:text>version {property[@name="xpathVersion"]/@value/string()} </xsl:text>

    <xsl:choose>
      <xsl:when test="property[@name='psviSupported']/@value = 'true'">
        <xsl:text>(PSVI support enabled)</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>(PSVI support disabled)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>.</xsl:text>
  </p>
</xsl:template>

<xsl:template match="testcase" mode="m:format-name">
  <xsl:param name="link" as="xs:boolean?" tunnel="yes"/>
  <xsl:variable name="name" select="replace(@name, '.xml', '')"/>
  <xsl:choose>
    <xsl:when test="$link = true()">
      <a href="https://test-suite.xproc.org/tests/{$name}.html">{$name}</a>
    </xsl:when>
    <xsl:otherwise>{$name}</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="testcase[skipped]">
  <tr class="skip">
    <td>{count(preceding-sibling::testcase)+1}</td>
    <td>
      <xsl:apply-templates select="." mode="m:format-name"/>
    </td>
    <td>skip</td>
    <td>-</td>
    <td>{skipped/node()}</td>
  </tr>
</xsl:template>

<xsl:template match="testcase[failure]">
  <tr class="fail">
    <td>{count(preceding-sibling::testcase)+1}</td>
    <td>
      <xsl:apply-templates select="." mode="m:format-name"/>
    </td>
    <td>fail</td>
    <td>
      <xsl:variable name="time" select="xs:double(@time)"/>
      <xsl:choose>
        <xsl:when test="$time lt 0">-</xsl:when>
        <xsl:otherwise>{format-number($time, '0.0000')}</xsl:otherwise>
      </xsl:choose>
    </td>
    <td>
      <xsl:value-of select="failure"/>
    </td>
  </tr>
</xsl:template>

<xsl:template match="testcase">
  <tr class="pass">
    <td>{count(preceding-sibling::testcase)+1}</td>
    <td>
      <xsl:apply-templates select="." mode="m:format-name"/>
    </td>
    <td>pass</td>
    <td>
      <xsl:variable name="time" select="xs:double(@time)"/>
      <xsl:choose>
        <xsl:when test="$time lt 0">-</xsl:when>
        <xsl:otherwise>{format-number($time, '0.0000')}</xsl:otherwise>
      </xsl:choose>
    </td>
    <td></td>
  </tr>
</xsl:template>

</xsl:stylesheet>
