<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:f="https://xmlcalabash.com/ns/functions/xsl"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:function name="f:hello" as="xs:string">
  <xsl:sequence select="'Hello, world.'"/>
</xsl:function>

</xsl:stylesheet>
