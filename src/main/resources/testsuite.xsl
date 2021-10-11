<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
		exclude-result-prefixes="xs"
                version="2.0">

<xsl:output method="html" encoding="utf-8" indent="no"
	    omit-xml-declaration="yes"/>

<xsl:param name="css" select="'MISSING'"/>
<xsl:param name="js" select="'MISSING'"/>

<xsl:template match="/">
  <html>
    <head>
      <title>XProc Test Suite Results</title>
      <link rel="stylesheet" type="text/css" href="data:text/css;base64,{$css}" />
      <script type="text/javascript" src="data:text/plain;base64,{$js}"></script>
    </head>
    <body>
      <xsl:apply-templates/>
    </body>
  </html>
</xsl:template>

<xsl:template match="testsuite">
  <div class="testsuite">
    <h1>Test Suite</h1>
    <h2>
      <span class="{if (@failures &gt; 0) then 'fail' else 'pass'}">
        <xsl:choose>
          <xsl:when test="@failures &gt; 0">
            <xsl:value-of select="@tests"/>
            <xsl:text> tests; </xsl:text>
            <xsl:value-of select="@failures"/>
            <xsl:text> failed.</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@tests"/>
            <xsl:text> tests.</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </span>
    </h2>

    <xsl:if test="@failures &gt; 0">
      <p><input type="checkbox" id="hidepassed"/> Hide passed tests.
      <input type="checkbox" id="hidefailed"/> Hide failed tests.</p>
    </xsl:if>

    <table class="properties">
      <tbody>
        <tr>
          <td class="pname">timestamp</td>
          <td class="pvalue"><xsl:value-of select="@timestamp"/></td>
        </tr>
        <xsl:apply-templates select="properties/property"/>
      </tbody>
    </table>

    <div class="testcases">
      <xsl:apply-templates/>
    </div>
  </div>
</xsl:template>

<xsl:template match="properties">
  <!-- suppress -->
</xsl:template>

<xsl:template match="property">
  <tr>
    <td class="pname"><xsl:value-of select="@name"/></td>
    <td class="pvalue"><xsl:value-of select="@value"/></td>
  </tr>
</xsl:template>

<xsl:template match="testcase">
  <div class="testcase { if (failure|error) then ' fail' else ' pass' }">
    <h2>
      <xsl:choose>
        <xsl:when test="contains(@name, '/test-suite/tests/')">
          <xsl:value-of select="substring-after(@name, '/test-suite/tests/')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
      </xsl:choose>
    </h2>
    <p>
      <xsl:choose>
        <xsl:when test="failure|error">
          <xsl:text>FAIL in </xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>PASS in </xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text> </xsl:text>
      <xsl:value-of select="@time"/>
      <xsl:text>s.</xsl:text>
    </p>
    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="error">
  <div class="error">
    <p class="title">
      <xsl:text>ERROR: </xsl:text>
      <xsl:value-of select="@message"/>
    </p>
    <pre>
      <xsl:value-of select="."/>
    </pre>
  </div>
</xsl:template>

<xsl:template match="failure">
  <div class="failure">
    <pre>
      <xsl:value-of select="."/>
    </pre>
  </div>
</xsl:template>

<xsl:template match="system-out|system-err">
  <xsl:if test="string(.) != ''">
    <div class="{local-name(.)}">
      <pre>
        <xsl:value-of select="."/>
      </pre>
    </div>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
