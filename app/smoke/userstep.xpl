<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:ex="http://example.com/steps"
                name="main"
                exclude-inline-prefixes="xs" version="3.0">
  <p:input port="source">
    <p:inline><doc>Success</doc></p:inline>
  </p:input>
  <p:output port="result"/>

  <p:declare-step type="ex:test">
    <p:input port="source"/>
    <p:output port="not-result"/>
    <p:identity/>
  </p:declare-step>

  <ex:test/>

</p:declare-step>
