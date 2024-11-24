<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main"
                exclude-inline-prefixes="#all" version="3.0">
  <p:input port="source">
    <p:inline><doc>7</doc></p:inline>
  </p:input>
  <p:output port="result"/>

  <p:try name="try">
    <p:choose>
      <p:when test="xs:integer(.) mod 2 = 0">
      <p:error code="cx:even"/>
    </p:when>
    <p:otherwise>
      <p:error code="cx:odd"/>
    </p:otherwise>
  </p:choose>
  <p:catch code="cx:odd">
    <p:identity>
      <p:with-input><doc>Successfully caught odd</doc></p:with-input>
    </p:identity>
  </p:catch>
  <p:catch code="cx:even">
    <p:identity>
      <p:with-input><doc>Successfully caught even</doc></p:with-input>
    </p:identity>
  </p:catch>
  <p:finally>
    <p:output port="finally" primary="false">
      <p:pipe step="final"/>
    </p:output>
    <p:identity name="final">
      <p:with-input><doc>Finally!</doc></p:with-input>
    </p:identity>
  </p:finally>
</p:try>

<p:wrap-sequence wrapper="both">
  <p:with-input>
    <p:pipe step="try"/>
    <p:pipe step="try" port="finally"/>
  </p:with-input>
</p:wrap-sequence>

</p:declare-step>
