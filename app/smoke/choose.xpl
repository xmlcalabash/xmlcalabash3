<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main"
                exclude-inline-prefixes="xs" version="3.0">
  <p:input port="source">
    <p:inline><doc>7</doc></p:inline>
  </p:input>
  <p:output port="result"/>

  <p:choose>
    <p:when test="string(.) = '4'">
      <p:identity>
        <p:with-input><doc>Success when 4</doc></p:with-input>
      </p:identity>
    </p:when>
    <p:when test="string(.) = '7'">
      <p:identity>
        <p:with-input><doc>Success when 7</doc></p:with-input>
      </p:identity>
    </p:when>
    <p:otherwise>
      <p:identity>
        <p:with-input><doc>Success when not 4 or 7 (was {string(.)})</doc></p:with-input>
      </p:identity>
    </p:otherwise>
  </p:choose>

</p:declare-step>
