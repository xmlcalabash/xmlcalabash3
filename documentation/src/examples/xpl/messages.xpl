<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                version="3.0">
  <p:import href="https://xmlcalabash.com/ext/library/pipeline-messages.xpl"/>

  <p:input port="source"/>
  <p:output port="result"
            serialization="map { 'method': 'xml', 'indent': 'yes' }"/>

  <p:xslt name="xslt">
    <p:with-input port="stylesheet">
      <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                      xmlns:xs="http://www.w3.org/2001/XMLSchema"
                      exclude-result-prefixes="xs"
                      version="3.0">
        <xsl:mode on-no-match="shallow-copy"/>
        <xsl:template match="/" name="xsl:initial-template">

          <xsl:message select="'Ran an identity transform.'"/>

          <xsl:apply-templates/>
        </xsl:template>
      </xsl:stylesheet>
    </p:with-input>
  </p:xslt>

  <cx:pipeline-messages p:depends="xslt" level="info"/>
</p:declare-step>
