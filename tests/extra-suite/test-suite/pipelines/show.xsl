<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:c="http://www.w3.org/ns/xproc-step"
                version="3.0"
                expand-text="yes">
  <xsl:param name="test" select="''" as="xs:string"/>

  <xsl:template name="main">
    <c:result>{$test}</c:result>
  </xsl:template>
</xsl:stylesheet>
