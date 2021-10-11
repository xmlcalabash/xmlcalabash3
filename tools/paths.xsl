<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:template match="/paths">
  <cases>
    <platform os="windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'file:///C:/Users/Jane%20Doe/Documents/'"/>
        <xsl:with-param name="drive" select="'C'"/>
        <xsl:with-param name="scheme" select="'file'"/>
        <xsl:with-param name="os" select="'windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'file:///D:/Users/Jane%20Doe/Documents/'"/>
        <xsl:with-param name="drive" select="'D'"/>
        <xsl:with-param name="scheme" select="'file'"/>
        <xsl:with-param name="os" select="'windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'file://hostname/Documents/'"/>
        <xsl:with-param name="authority" select="'hostname'"/>
        <xsl:with-param name="scheme" select="'file'"/>
        <xsl:with-param name="os" select="'windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'http://example.com/Documents/'"/>
        <xsl:with-param name="scheme" select="'http'"/>
        <xsl:with-param name="os" select="'windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'file:///home/jdoe/documents/'"/>
        <xsl:with-param name="scheme" select="'file'"/>
        <xsl:with-param name="os" select="'windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN'"/>
        <xsl:with-param name="hierarchical" select="'false'"/>
        <xsl:with-param name="os" select="'windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'file:not-absolute'"/>
        <xsl:with-param name="os" select="'windows'"/>
        <xsl:with-param name="absolute" select="false()"/>
      </xsl:apply-templates>
    </platform>
    <platform os="non-windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'file:///home/jdoe/documents/'"/>
        <xsl:with-param name="scheme" select="'file'"/>
        <xsl:with-param name="os" select="'non-windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="non-windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'http://example.com/documents/'"/>
        <xsl:with-param name="scheme" select="'http'"/>
        <xsl:with-param name="os" select="'non-windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="non-windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'urn:publicid:ISO+8879%3A1986:ENTITIES+Added+Latin+1:EN'"/>
        <xsl:with-param name="hierarchical" select="'false'"/>
        <xsl:with-param name="os" select="'non-windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="non-windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'file://hostname/Documents/'"/>
        <xsl:with-param name="authority" select="'hostname'"/>
        <xsl:with-param name="scheme" select="'file'"/>
        <xsl:with-param name="os" select="'non-windows'"/>
      </xsl:apply-templates>
    </platform>
    <platform os="non-windows">
      <xsl:apply-templates select="path">
        <xsl:with-param name="basedir" select="'file:not-absolute'"/>
        <xsl:with-param name="os" select="'non-windows'"/>
        <xsl:with-param name="absolute" select="false()"/>
      </xsl:apply-templates>
    </platform>
  </cases>
</xsl:template>

<xsl:template match="path">
  <xsl:param name="basedir" select="()" as="xs:string?"/>
  <xsl:param name="drive" select="()" as="xs:string?"/>
  <xsl:param name="authority" select="()" as="xs:string?"/>
  <xsl:param name="scheme" select="()" as="xs:string?"/>
  <xsl:param name="hierarchical" select="'true'" as="xs:string"/>
  <xsl:param name="os" as="xs:string" required="true"/>
  <xsl:param name="absolute" as="xs:boolean" select="true()"/>

  <xsl:variable name="ascheme" as="xs:string?">
    <xsl:choose>
      <xsl:when test="not(@scheme)">
        <xsl:sequence select="()"/>
      </xsl:when>
      <xsl:when test="starts-with(., 'file:')">
        <xsl:sequence select="'file'"/>
      </xsl:when>
      <xsl:when test="$os = 'non-windows' and @drive">
        <xsl:sequence select="string(@drive)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="string(@scheme)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="drive" as="attribute()?">
    <xsl:choose>
      <xsl:when test="$os = 'non-windows' and @drive">
        <xsl:sequence select="()"/>
      </xsl:when>
      <xsl:when test="contains($basedir, 'D:')">
        <xsl:attribute name="drive" select="'D'"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="@drive"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="path" as="attribute()?">
    <xsl:choose>
      <xsl:when test="$os = 'non-windows' and starts-with(., 'file:C:')">
        <xsl:attribute name="path" select="'relative'"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="@path"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="implicit" as="attribute()?">
    <xsl:choose>
      <xsl:when test="$os = 'non-windows' and starts-with(., 'C:')">
        <xsl:sequence select="()"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="@implicit"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="error" as="xs:string?">
    <xsl:choose>
      <xsl:when test="$path = 'relative' and not($hierarchical eq 'true')">err:XD0080</xsl:when>
      <xsl:when test="$os = 'windows' and @path = 'relative'
                      and starts-with($basedir, 'file:not')">err:XD0074</xsl:when>
      <xsl:when test="$os = 'windows' and @path = 'relative'
                      and @drive and @drive != $drive">err:XD0075</xsl:when>
      <xsl:when test="$os = 'windows' and @path = 'relative'
                      and @drive and not(contains($basedir, 'C:'))
                                 and not(contains($basedir, 'D:'))">err:XD0075</xsl:when>
      <xsl:when test="$os = 'windows' and @path = 'relative'
                      and ((@drive and exists($authority)) or (@authority and exists($drive)))
                      ">err:XD0076</xsl:when>
      <xsl:when test="$path = 'relative' and @scheme and $ascheme != $scheme">err:XD0077</xsl:when>
      <xsl:when test="$path = 'relative' and starts-with($basedir, 'http') and @scheme = 'file'">err:XD0077</xsl:when>
      <xsl:when test="not(@scheme) and not($hierarchical eq 'true')">err:XD0080</xsl:when>
      <xsl:when test="$path = 'relative' and not($absolute)">err:XD0074</xsl:when>

      <!-- special case -->
      <xsl:when test="$implicit = 'true'
                      and (($path = 'relative' and not($absolute))
                           or starts-with($basedir, 'file:not'))
                      and $os = 'windows' and starts-with(., 'C:/')"
                ></xsl:when>

      <xsl:when test="$implicit = 'true'
                      and (($path = 'relative' and not($absolute))
                           or starts-with($basedir, 'file:not'))"
                >err:XD0074</xsl:when>
      <xsl:when test="$os = 'windows' and @authority = 'true'
                      and not(starts-with(., 'file://authority.com/'))
                      and (contains($basedir, 'C:') or contains($basedir, 'D:'))">err:XD0076</xsl:when>
    </xsl:choose>
  </xsl:variable>

  <case>
    <xsl:sequence select="@* except (@scheme, @drive, @path, @implicit)"/>
    <xsl:if test="exists($ascheme)">
      <xsl:attribute name="scheme" select="$ascheme"/>
    </xsl:if>
    <xsl:sequence select="$drive"/>
    <xsl:sequence select="$path"/>
    <xsl:sequence select="$implicit"/>

    <xsl:if test="exists($error)">
      <xsl:attribute name="error" select="$error"/>
    </xsl:if>
    <filepath><xsl:sequence select="string(.)"/></filepath>
    <xsl:if test="exists($basedir)">
      <basedir><xsl:sequence select="$basedir"/></basedir>
    </xsl:if>
    <xsl:if test="empty($error)">
      <xsl:choose>
        <xsl:when test="@hierarchical = 'false'">
          <result x="0"><xsl:sequence select="string(.)"/></result>
        </xsl:when>
        <xsl:when test="@scheme and $path = 'absolute'">
          <result x="1">
            <xsl:choose>
              <xsl:when test="$ascheme = 'file' and $implicit='true'
                              and starts-with(., 'file:///')">
                <xsl:sequence select="replace(., ' ', '%20')"/>
              </xsl:when>
              <xsl:when test="$ascheme = 'file' and $implicit='true'">
                <xsl:sequence select="'file:///' || replace(., ' ', '%20')"/>
              </xsl:when>
              <xsl:when test="starts-with(., 'file:/path')">
                <xsl:sequence select="'file://' || substring-after(., 'file:')"/>
              </xsl:when>
              <xsl:when test="starts-with(., 'file:C:')">
                <xsl:sequence select="'file:///C:' || substring-after(., 'file:C:')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="string(.)"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="$path = 'absolute'
                        and (starts-with($basedir, 'file:///C:')
                             or starts-with($basedir, 'file:///D:'))">
          <result x="2">
            <xsl:choose>
              <xsl:when test="$implicit='true'">
                <xsl:sequence select="substring($basedir, 1, 10) || replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="substring($basedir, 1, 10) || replace(., '^/+', '/')"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="$path = 'absolute' and starts-with($basedir, 'http://example.com/')">
          <result x="3">
            <xsl:choose>
              <xsl:when test="$implicit='true' and @authority = 'true'
                              and starts-with($basedir, 'http:')">
                <xsl:sequence select="'http:' || ."/>
              </xsl:when>
              <xsl:when test="$implicit='true'">
                <xsl:sequence select="'http://example.com' || replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="'http://example.com' || replace(., '^/+', '/')"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="@authority = 'true' and $os='windows' and starts-with($basedir, 'file://')">
          <result x="4">
            <xsl:choose>
              <xsl:when test="$implicit='true'">
                <xsl:sequence select="'file:/' || replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:when test="starts-with(., 'file:')">
                <xsl:sequence select="replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="'file:/' || replace(., '^/+', '/')"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="@authority = 'true' and starts-with($basedir, 'file://')">
          <result x="5">
            <xsl:choose>
              <xsl:when test="$implicit='true'">
                <xsl:sequence select="'file:/' || replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="string(.)"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="$path = 'absolute' and starts-with($basedir, 'file:///')">
          <result x="6">
            <xsl:choose>
              <xsl:when test="$implicit='true'">
                <xsl:sequence select="'file://' || replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="'file://' || replace(., '^/+', '/')"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="$os = 'windows' and $path = 'absolute' and starts-with($basedir, 'file://hostname/')">
          <result x="7">
            <xsl:choose>
              <xsl:when test="$implicit='true'">
                <xsl:sequence select="'file://hostname' || replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="'file://hostname' || replace(., '^/+', '/')"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="$os = 'non-windows' and $path = 'absolute' and starts-with($basedir, 'file://hostname/')">
          <result x="8">
            <xsl:choose>
              <xsl:when test="$implicit='true' and starts-with($basedir, 'file://hostname/')">
                <xsl:sequence select="'file://hostname' || replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:when test="$implicit='true'">
                <xsl:sequence select="'file://' || replace(replace(., ' ', '%20'), '^/+', '/')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="'file://' || replace(., '^/+', '/')"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="$path = 'relative'">
          <result x="9">
            <xsl:choose>
              <xsl:when test="$implicit='true' and @authority = 'true'
                              and starts-with($basedir, 'http:')">
                <xsl:sequence select="'http:' || ."/>
              </xsl:when>
              <xsl:when test="$os = 'windows'
                              and (starts-with(., 'file:C:') or starts-with(., 'file:///C:'))">
                <xsl:sequence select="$basedir || substring-after(., 'C:')"/>
              </xsl:when>                              
              <xsl:when test="$os = 'windows' and starts-with(., 'C:')">
                <xsl:sequence select="$basedir || replace(substring-after(., 'C:'), ' ', '%20')"/>
              </xsl:when>                              
              <xsl:when test="$implicit='true'">
                <xsl:sequence select="$basedir || replace(., ' ', '%20')"/>
              </xsl:when>
              <xsl:when test="starts-with(.,'file:')">
                <xsl:sequence select="$basedir || substring-after(., 'file:')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="$basedir || string(.)"/>
              </xsl:otherwise>
            </xsl:choose>
          </result>
        </xsl:when>
        <xsl:when test="$path = 'absolute'">
          <xsl:choose>
            <xsl:when test="$implicit = 'true'">
              <result x="10">file://<xsl:sequence select="string(.)"/></result>
            </xsl:when>
            <xsl:otherwise>
              <result x="10"><xsl:sequence select="string(.)"/></result>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message select="'Warning: unhandled case.'"/>
          <result>ERROR</result>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </case>
</xsl:template>    

</xsl:stylesheet>
