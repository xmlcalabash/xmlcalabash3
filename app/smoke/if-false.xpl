<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main"
                exclude-inline-prefixes="xs" version="3.0">
  <p:input port="source">
    <p:inline><doc>7</doc></p:inline>
  </p:input>
  <p:output port="result" sequence="true"/>

  <p:if name="if" test="xs:integer(.) mod 2 = 0">
    <p:output port="r1" primary="true"/>
    <p:output port="r2">
      <p:pipe step="r2"/>
    </p:output>
    <p:identity name="r2">
      <p:with-input><doc>This is a secondary result.</doc></p:with-input>
    </p:identity>
    <p:identity>
      <p:with-input><doc>Success if input was even.</doc></p:with-input>
    </p:identity>
  </p:if>

  <p:wrap-sequence wrapper="both">
    <p:with-input>
      <p:pipe step="if"/>
      <p:pipe step="if" port="r2"/>
    </p:with-input>
  </p:wrap-sequence>

</p:declare-step>
