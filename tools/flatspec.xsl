<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="text" encoding="utf-8" indent="no"/>

<xsl:param name="os" select="'non-windows'"/>

<xsl:template match="/">
  <xsl:apply-templates select="//path"/>
  <xsl:apply-templates select="/cases/platform[@os = $os]/case"/>
</xsl:template>

<xsl:template match="path" expand-text="yes">
  <xsl:text>"{.} " should " parse" in {{&#10;</xsl:text>
  <xsl:text>  val path = new Urify("{.}")&#10;</xsl:text>
  <xsl:choose>
    <xsl:when test="$os = 'non-windows' and starts-with(., 'C:')">
      <xsl:text>  assert(path.scheme.isDefined)&#10;</xsl:text>
      <xsl:text>  assert(path.scheme.get == "C")&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="@scheme">
      <xsl:text>  assert(path.scheme.isDefined)&#10;</xsl:text>
      <xsl:text>  assert(path.scheme.get == "{@scheme}")&#10;</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  assert(path.scheme.isEmpty)&#10;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
    <xsl:when test="$os = 'non-windows' and starts-with(., 'C:')">
      <xsl:text>  assert(path.explicit)&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="@implicit = 'true'">
      <xsl:text>  assert(!path.explicit)&#10;</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  assert(path.explicit)&#10;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
    <xsl:when test="@hierarchical = 'false'">
      <xsl:text>  assert(!path.hierarchical)&#10;</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  assert(path.hierarchical)&#10;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
    <xsl:when test="@authority = 'true'">
      <xsl:text>  assert(path.authority.isDefined)&#10;</xsl:text>
      <xsl:choose>
        <xsl:when test="starts-with(., '//authority')">
          <xsl:text>  assert(path.authority.get == "authority")&#10;</xsl:text>
        </xsl:when>
        <xsl:when test="starts-with(., 'file://authority.com')">
          <xsl:text>  assert(path.authority.get == "authority.com")&#10;</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>  assert(false)&#10;</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  assert(path.authority.isEmpty)&#10;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
    <xsl:when test="not($os = 'windows')">
      <xsl:text>  assert(path.driveLetter.isEmpty)&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="@drive">
      <xsl:text>  assert(path.driveLetter.isDefined)&#10;</xsl:text>
      <xsl:text>  assert(path.driveLetter.get == "{@drive}")&#10;</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  assert(path.driveLetter.isEmpty)&#10;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>


  <xsl:choose>
    <xsl:when test="@path = 'absolute' and not($os = 'windows')
                    and starts-with(., 'file:C:')">
      <xsl:text>  assert(!path.absolute)&#10;</xsl:text>
      <xsl:text>  assert(path.relative)&#10;</xsl:text>
      <xsl:text>  assert(path.path == "{substring-after(., 'file:')}")&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="@path = 'absolute'">
      <xsl:text>  assert(path.absolute)&#10;</xsl:text>
      <xsl:text>  assert(!path.relative)&#10;</xsl:text>

      <xsl:variable name="path" as="xs:string">
        <xsl:choose>
          <xsl:when test="starts-with(., '//authority')">
            <xsl:sequence select="substring-after(., '//authority')"/>
          </xsl:when>
          <xsl:when test="starts-with(., 'file://authority.com')">
            <xsl:sequence select="substring-after(., 'file://authority.com')"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:sequence select="substring-after(., '/')"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:variable name="path" select="replace($path, '^/+', '')"/>

      <xsl:choose>
        <xsl:when test="starts-with(., 'https://')">
          <xsl:text>  assert(path.path == "{substring-after(., 'https:')}")&#10;</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>  assert(path.path == "/{$path}")&#10;</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  assert(!path.absolute)&#10;</xsl:text>
      <xsl:text>  assert(path.relative)&#10;</xsl:text>

      <xsl:variable name="path" as="xs:string">
        <xsl:choose>
          <xsl:when test="$os = 'windows'
                          and (starts-with(., 'C:') or starts-with(., 'file:C:'))">
            <xsl:sequence select="substring-after(., 'C:')"/>
          </xsl:when>
          <xsl:when test=". = 'https:'">
            <xsl:sequence select="''"/>
          </xsl:when>
          <xsl:when test="starts-with(., 'urn:')">
            <xsl:sequence select="substring-after(., 'urn:')"/>
          </xsl:when>
          <xsl:when test=". = 'file://authority.com'">
            <xsl:sequence select="''"/>
          </xsl:when>
          <xsl:when test="not($os = 'windows') and starts-with(., 'C:')">
            <xsl:sequence select="substring-after(., 'C:')"/>
          </xsl:when>
          <xsl:when test="starts-with(., 'file:')">
            <xsl:sequence select="substring-after(., 'file:')"/>
          </xsl:when>
          <xsl:when test=". = '//authority'">
            <xsl:sequence select="''"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:sequence select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:text>  assert(path.path == "{$path}")&#10;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>}}&#10;&#10;</xsl:text>
</xsl:template>

<xsl:template match="case" expand-text="yes">
  <xsl:text>"{filepath} " should " resolve against {basedir}" in {{&#10;</xsl:text>
  <xsl:text>  val basepath = new Urify("{basedir}")&#10;</xsl:text>
  <xsl:text>  val path = basepath.resolve("{filepath}")&#10;</xsl:text>
  <xsl:text>  assert(path == "{result}")&#10;</xsl:text>
  <xsl:text>}}&#10;&#10;</xsl:text>
</xsl:template>

<xsl:template match="case[@error]" expand-text="yes">
  <xsl:text>"{filepath} " should " throw an exception against {basedir}" in {{&#10;</xsl:text>
  <xsl:text>  val basepath = new Urify("{basedir}")&#10;</xsl:text>
  <xsl:text>  try {{&#10;</xsl:text>
  <xsl:text>  basepath.resolve("{filepath}")&#10;</xsl:text>
  <xsl:text>  fail()&#10;</xsl:text>
  <xsl:text>  }} catch {{&#10;</xsl:text>
  <xsl:text>  case ex: XProcException =>&#10;</xsl:text>
  <xsl:text>      assert(ex.code.getLocalName == "{substring-after(@error, 'err:')}")&#10;</xsl:text>
  <xsl:text>  case _ => fail()&#10;</xsl:text>
  <xsl:text>  }}&#10;</xsl:text>
  <xsl:text>}}&#10;&#10;</xsl:text>
</xsl:template>

</xsl:stylesheet>
