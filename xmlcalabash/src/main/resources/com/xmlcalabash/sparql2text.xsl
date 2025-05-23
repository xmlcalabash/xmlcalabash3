<?xml version="1.1" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:r="http://www.w3.org/2005/sparql-results#"
                xmlns:f="http://xmlcalabash.com/ns/functions"
                version="3.0" expand-text="yes">

<xsl:output method="text" encoding="utf-8" indent="no"/>

<xsl:param name="page-length" as="xs:integer?" select="()"/>
<xsl:param name="number-rows" as="xs:boolean" select="false()"/>
<xsl:param name="formfeed" as="xs:string" select="'&#12;'"/>
<xsl:param name="nl" as="xs:string" select="'&#10;'"/>

<xsl:template match="r:sparql">
  <xsl:if test="exists($page-length) and $page-length lt 5">
    <xsl:message select="'Page length must be greater than 4'"/>
  </xsl:if>

  <xsl:variable name="cols"
                select="count(r:head/r:variable)
                        + (if ($number-rows) then 1 else 0)"/>
  <xsl:variable name="rows" select="count(r:results/r:result)"/>

  <xsl:variable name="columns"
                select="((if ($number-rows) then '#' else ()),
                         r:head/r:variable/@name ! string(.))"/>
  <xsl:variable name="results" select="r:results/r:result"/>

  <xsl:variable name="widths" as="xs:integer*">
    <xsl:if test="$number-rows">
      <xsl:sequence select="string-length(string($rows))"/>
    </xsl:if>
    <xsl:for-each select="r:head/r:variable">
      <xsl:variable name="name" select="string(@name)"/>
      <xsl:variable name="col" select="position()"/>

      <xsl:variable name="widths" as="xs:integer+">
        <xsl:sequence select="string-length($name)"/>
        <xsl:for-each select="$results/r:binding[@name = $name]">
          <xsl:sequence select="string-length(*/text())"/>
        </xsl:for-each>
      </xsl:variable>
      <xsl:sequence select="max($widths)+2"/>
    </xsl:for-each>
  </xsl:variable>

  <xsl:for-each select="$results">
    <xsl:choose>
      <xsl:when test="position() = 1">
        <xsl:value-of select="f:divider($widths)"/>
        <xsl:value-of select="f:row($columns, $widths)"/>
        <xsl:value-of select="f:divider($widths)"/>
      </xsl:when>
      <xsl:when test="exists($page-length) and (position() - 1) mod $page-length = 0">
        <xsl:value-of select="f:divider($widths)"/>
        <xsl:if test="exists($page-length)">
          <xsl:value-of select="$formfeed"/>
        </xsl:if>
        <xsl:value-of select="f:divider($widths)"/>
        <xsl:value-of select="f:row($columns, $widths)"/>
        <xsl:value-of select="f:divider($widths)"/>
      </xsl:when>
      <xsl:otherwise/>
    </xsl:choose>

    <xsl:variable name="bindings" select="r:binding"/>
    <xsl:variable name="cols" as="xs:string+">
      <xsl:for-each select="$columns">
        <xsl:variable name="name" select="."/>
        <xsl:sequence select="$bindings[@name = $name]/*/text()"/>
      </xsl:for-each>
    </xsl:variable>
    <xsl:value-of select="f:row(((if ($number-rows) then string(position()) else ()), $cols), $widths)"/>
  </xsl:for-each>

  <xsl:value-of select="f:divider($widths)"/>
</xsl:template>

<xsl:function name="f:divider" as="xs:string">
  <xsl:param name="widths" as="xs:integer+"/>

  <xsl:variable name="parts" as="xs:string+">
    <xsl:for-each select="$widths">
      <xsl:choose>
        <xsl:when test="position() = 1">
          <xsl:text>|-</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>-+-</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:sequence select="f:string-of('-', .)"/>
    </xsl:for-each>
    <xsl:text>-|</xsl:text>
  </xsl:variable>

  <xsl:sequence select="string-join($parts, '') || $nl"/>
</xsl:function>

<xsl:function name="f:row" as="xs:string">
  <xsl:param name="values" as="xs:string+"/>
  <xsl:param name="widths" as="xs:integer+"/>

  <xsl:variable name="parts" as="xs:string+">
    <xsl:for-each select="$widths">
      <xsl:variable name="col" select="position()"/>
      <xsl:choose>
        <xsl:when test="$col = 1">
          <xsl:text>| </xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text> | </xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:sequence select="f:pad($values[$col], $widths[$col])"/>
    </xsl:for-each>
    <xsl:text> |</xsl:text>
  </xsl:variable>

  <xsl:sequence select="string-join($parts, '') || $nl"/>
</xsl:function>

<xsl:function name="f:pad" as="xs:string">
  <xsl:param name="value" as="xs:string?"/>
  <xsl:param name="length" as="xs:integer"/>

  <xsl:choose>
    <xsl:when test="string-length($value) ge $length">
      <xsl:sequence select="substring($value, 1, $length)"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="pad" select="$length - string-length($value)"/>
      <xsl:sequence select="$value || f:string-of(' ', $pad)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<xsl:function name="f:string-of" as="xs:string">
  <xsl:param name="chars" as="xs:string"/>
  <xsl:param name="length" as="xs:integer"/>

  <xsl:variable name="next" select="$chars || $chars"/>

  <xsl:choose>
    <xsl:when test="string-length($next) ge $length">
      <xsl:sequence select="substring($next, 1, $length)"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="f:string-of($next, $length)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

</xsl:stylesheet>
