<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xvrl="http://www.xproc.org/ns/xvrl"
  xmlns="http://www.xproc.org/ns/xvrl"
  version="2.0"
  exclude-result-prefixes="#all">

  <xsl:template match="/">
    <xsl:apply-templates select=".//svrl:schematron-output" mode="svrl2xvrl"/>
  </xsl:template>

  <xsl:param name="default-severity" as="xs:string" select="'error'"/>

  <xsl:template match="svrl:schematron-output" mode="svrl2xvrl">
    <report>
      <metadata>
        <schema schematypens="http://purl.oclc.org/dsdl/schematron"/>
        <xsl:apply-templates select="svrl:active-pattern[1]/@document" mode="#current"/>
      </metadata>
      <xsl:for-each-group select="*" group-starting-with="svrl:active-pattern">
        <xsl:if test="exists(current-group()[1]/self::svrl:active-pattern)">
          <xsl:variable name="pattern" as="element(svrl:active-pattern)" select="."/>
          <xsl:for-each-group select="current-group()[position() gt 1]" group-starting-with="svrl:fired-rule">
            <xsl:apply-templates select="current-group()/(self::svrl:successful-report | self::svrl:failed-assert)" mode="#current">
              <xsl:with-param name="rule" tunnel="yes" as="element(svrl:fired-rule)" select="."/>
              <xsl:with-param name="pattern" tunnel="yes" as="element(svrl:active-pattern)" select="$pattern"/>
            </xsl:apply-templates>
          </xsl:for-each-group>
        </xsl:if>
      </xsl:for-each-group>
    </report>
  </xsl:template>
  
  <xsl:template match="svrl:failed-assert | svrl:successful-report" mode="svrl2xvrl">
    <xsl:param name="rule" as="element(svrl:fired-rule)" tunnel="yes"/>
    <detection>
      <xsl:attribute name="severity" select="$default-severity"/>
      <xsl:apply-templates select="@role" mode="#current"/>
      <location>
        <xsl:attribute name="xpath" select="@location"/>
      </location>
      <xsl:apply-templates select="$rule/@context" mode="#current"/>
      <xsl:apply-templates select="svrl:text, svrl:diagnostic-reference" mode="#current"/>
    </detection>
  </xsl:template>
  
  <xsl:template match="@role" mode="svrl2xvrl">
    <xsl:attribute name="severity" select="."/>
  </xsl:template>
  
  <xsl:template match="@document" mode="svrl2xvrl">
    <document href="{.}"/>
  </xsl:template>
  
  <xsl:template match="@context" mode="svrl2xvrl">
    <context>
      <location xpath="{.}"/>
    </context>
  </xsl:template>
  
  <xsl:template match="svrl:text | svrl:diagnostic-reference" mode="svrl2xvrl">
    <message>
      <xsl:apply-templates select="(@xml:lang, ../@xml:lang)[1], node()" mode="#current"/>
    </message>
  </xsl:template>
  
  <xsl:template match="@xml:lang" mode="svrl2xvrl">
    <xsl:copy/>
  </xsl:template>
  
</xsl:stylesheet>