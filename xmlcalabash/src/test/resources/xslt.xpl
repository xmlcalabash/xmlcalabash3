<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main" version="3.0">
  <p:input port="source"/>
  <p:output port="result"/>

  <p:option name="message" as="xs:string?" select="()"/>
  <p:option name="fail" as="xs:boolean" select="false()"/>

  <p:group>
    <p:output port="result" pipe="@xslt"/>

    <p:xslt name="xslt" parameters="map { 'message': $message, 'fail': $fail }">
      <p:with-input port="stylesheet">
        <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                        xmlns:xs="http://www.w3.org/2001/XMLSchema"
                        exclude-result-prefixes="xs"
                        version="3.0">
          <xsl:output method="xml" encoding="utf-8" indent="no"/>
          <xsl:param name="message" as="xs:string?" select="()"/>
          <xsl:param name="fail" as="xs:boolean" select="false()"/>

          <xsl:mode on-no-match="shallow-copy"/>

          <xsl:template match="/">
            <xsl:if test="exists($message)">
              <xsl:message select="$message"/>
            </xsl:if>
            <xsl:apply-templates/>
          </xsl:template>

          <xsl:template match="/*">
            <xsl:if test="$fail">
              <xsl:sequence select="error(xs:QName('BANG'), 'It went bang')"/>
            </xsl:if>
            <doc>You got some output</doc>
          </xsl:template>
        </xsl:stylesheet>
      </p:with-input>
    </p:xslt>

    <p:sink>
      <p:with-input pipe="secondary@xslt"/>
    </p:sink>
  </p:group>

</p:declare-step>
