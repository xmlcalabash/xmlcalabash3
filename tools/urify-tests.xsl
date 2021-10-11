<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:t="http://xproc.org/ns/testsuite/3.0"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:template match="cases">
  <cases>
    <xsl:apply-templates select="//case"/>
  </cases>
</xsl:template>

<xsl:template match="case" expand-text="yes">
  <xsl:variable name="os" select="../@os/string()"/>
  <xsl:variable name="num" select="count(preceding::case[../@os = $os])+1"/>
  <xsl:variable name="feature"
                select="if ($os = 'windows') then 'urify-windows' else 'urify-non-windows'"/>
  <xsl:variable name="name" select="'nw-' || $feature"/>

  <xsl:result-document href="{$name}-{format-number($num, '001')}.xml">
    <t:test xmlns:t="http://xproc.org/ns/testsuite/3.0"
            features="{$feature}">
      <xsl:choose>
        <xsl:when test="@error">
          <xsl:namespace name="err" select="'http://www.w3.org/ns/xproc-error'"/>
          <xsl:attribute name="expected" select="'fail'"/>
          <xsl:attribute name="code" select="@error"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="expected" select="'pass'"/>
        </xsl:otherwise>
      </xsl:choose>
      <t:info>
        <t:title>{$name}-{format-number($num, '001')}</t:title>
        <t:revision-history>
          <t:revision>
            <t:date>{string(@date)}</t:date>
            <t:author>
              <t:name>Norman Walsh</t:name>
            </t:author>
            <t:description xmlns="http://www.w3.org/1999/xhtml">
              <xsl:choose>
                <xsl:when test="$feature = 'urify-non-windows'">
                  <p>Test for the <code>p:urify()</code> function not on Windows.</p>
                </xsl:when>
                <xsl:otherwise>
                  <p>Test for the <code>p:urify()</code> function on Windows.</p>
                </xsl:otherwise>
              </xsl:choose>
            </t:description>
          </t:revision>
        </t:revision-history>
      </t:info>
      <t:description xmlns="http://www.w3.org/1999/xhtml">
        <p>
          <xsl:text>Resolve a string with </xsl:text>
          <xsl:choose>
            <xsl:when test="$os = 'non-windows' and @drive">
              <xsl:text>a bogus URI scheme </xsl:text>
              <xsl:choose>
                <xsl:when test="@path = 'absolute'">with an absolute path</xsl:when>
                <xsl:otherwise>with a relative path</xsl:otherwise>
              </xsl:choose>
            </xsl:when>
            <xsl:when test="@scheme">
              <xsl:choose>
                <xsl:when test="@implicit = 'true'">an implicit</xsl:when>
                <xsl:otherwise>an explicit</xsl:otherwise>
              </xsl:choose>
              <xsl:if test="@hierarchical = 'false'">, non-hierarchical</xsl:if>
              <xsl:sequence select="' ' || @scheme || ' URI scheme '"/>
              <xsl:if test="not(@hierarchical = 'false')">
                <xsl:choose>
                  <xsl:when test="@path = 'absolute'">with an absolute path</xsl:when>
                  <xsl:otherwise>with a relative path</xsl:otherwise>
                </xsl:choose>
              </xsl:if>
              <xsl:text>&#10;</xsl:text>
              <xsl:if test="$os='windows' and @drive"> on drive {@drive}:</xsl:if>
              <xsl:if test="$os='windows' and @authority"> with an authority</xsl:if>
            </xsl:when>
            <xsl:otherwise>
              <xsl:if test="@implicit = 'true'">an implicit</xsl:if>
              <xsl:if test="@hierarchical = 'false'">, non-hierarchical </xsl:if>
              <xsl:text> scheme </xsl:text>
              <xsl:if test="not(@hierarchical = 'false')">
                <xsl:choose>
                  <xsl:when test="@path = 'absolute'">with an absolute path</xsl:when>
                  <xsl:otherwise>with a relative path</xsl:otherwise>
                </xsl:choose>
              </xsl:if>
              <xsl:text>&#10;</xsl:text>
              <xsl:if test="os='windows' and @drive"> on drive {@drive}:</xsl:if>
              <xsl:if test="$os='windows' and @authority"> with an authority</xsl:if>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text> against </xsl:text>
          <xsl:choose>
            <xsl:when test="starts-with(basedir, 'file:///C:')">
              <xsl:text>an absolute file URI on drive C:.</xsl:text>
            </xsl:when>
            <xsl:when test="starts-with(basedir, 'file:///D:')">
              <xsl:text>an absolute file URI on drive D:.</xsl:text>
            </xsl:when>
            <xsl:when test="starts-with(basedir, 'file:///home')">
              <xsl:text>an absolute file URI.</xsl:text>
            </xsl:when>
            <xsl:when test="starts-with(basedir, 'file://hostname')">
              <xsl:choose>
                <xsl:when test="$os = 'windows'">
                  <xsl:text>an absolute file URI with an authority.</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:text>an absolute file URI with an authority.</xsl:text>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:when>
            <xsl:when test="starts-with(basedir, 'file:not-absolute')">
              <xsl:text>a relative file URI.</xsl:text>
            </xsl:when>
            <xsl:when test="starts-with(basedir, 'http:/')">
              <xsl:text>an absolute http: URI.</xsl:text>
            </xsl:when>
            <xsl:when test="starts-with(basedir, 'urn:')">
              <xsl:text>a non-hierarchical urn URI.</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:message>Failed to recognize basedir: {string(basedir)}</xsl:message>
            </xsl:otherwise>
          </xsl:choose>
        </p>
      </t:description>
      <t:pipeline>
<p:declare-step name="pipeline"
                version="3.0"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <p:output port="result"/>
  <p:identity>
    <p:with-input>
      <result>{{p:urify("{filepath}",
                             "{basedir}")}}</result>
    </p:with-input>
  </p:identity>
</p:declare-step>
      </t:pipeline>
      <xsl:if test="empty(@error)">
        <t:schematron>
          <s:schema queryBinding="xslt2"
                    xmlns:s="http://purl.oclc.org/dsdl/schematron"
                    xmlns="http://www.w3.org/1999/xhtml">
            <s:pattern>
              <s:rule context="/">
                <s:assert test="result">Root element is not 'result'.</s:assert>
                <s:assert test="string(result)= '{result}'"
                          >Incorrect URI in result.</s:assert>
              </s:rule>
            </s:pattern>
          </s:schema>
        </t:schematron>
      </xsl:if>
    </t:test>
  </xsl:result-document>
</xsl:template>

</xsl:stylesheet>
