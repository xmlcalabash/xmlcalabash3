<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main"
                exclude-inline-prefixes="xs" version="3.0">
  <p:input port="source">
    <p:inline><doc>One</doc></p:inline>
    <p:inline><doc>Two</doc></p:inline>
  </p:input>
  <p:output port="result" sequence="true"/>
  <p:for-each>
    <p:add-attribute match="/*" attribute-name="success" attribute-value="'true'"/>
  </p:for-each>
</p:declare-step>
