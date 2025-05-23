<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step" 
                version="3.0" >
    
  <p:input port="source" sequence="true" primary="true"/>
  <p:output port="result" primary="true" sequence="true"/>
    
  <p:xslt template-name="main">
    <p:with-input port="stylesheet">
      <p:inline>
        <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">
          <xsl:variable name="action" select="'test'"/>
          <xsl:template name="main">
            <result action="{$action}"/>
          </xsl:template>
        </xsl:stylesheet>
      </p:inline>            
    </p:with-input>
  </p:xslt>
</p:declare-step>
