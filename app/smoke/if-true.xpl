<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main"
                exclude-inline-prefixes="xs" version="3.0">
  <p:input port="source">
    <p:inline><doc>6</doc></p:inline>
  </p:input>
  <p:output port="result" sequence="true"/>

<!--
  <p:choose name="if">
    <p:when test="xs:integer(.) mod 2 = 0">
      <p:identity>
        <p:with-input><doc>Success if input was even.</doc></p:with-input>
      </p:identity>
    </p:when>
    <p:otherwise>
      <p:identity>
        <p:with-input>
          <p:empty/>
        </p:with-input>
      </p:identity>
    </p:otherwise>
  </p:choose>
-->

  <p:if name="if" test="xs:integer(.) mod 2 = 0">
    <p:identity>
      <p:with-input><doc>Success if input was even.</doc></p:with-input>
    </p:identity>
  </p:if>

  <p:identity name="primin">
    <p:with-input pipe="@main"/>
  </p:identity>

  <p:identity name="copy">
    <p:with-input><doc>Testing with {string(.)}</doc></p:with-input>
  </p:identity>

  <p:wrap-sequence wrapper="both">
    <p:with-input>
      <p:pipe step="copy"/>
      <p:pipe step="if"/>
    </p:with-input>
  </p:wrap-sequence>

</p:declare-step>
