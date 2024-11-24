<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:i="http://example.com/import/test"
           version="3.0">

<p:declare-step type="i:c">
  <p:import href="D.xpl"/>
  <p:output port="result" />
  <p:identity>
    <p:with-input use-when='false()'><doc>this is not i:c</doc></p:with-input>
    <p:with-input><doc>this is i:c</doc></p:with-input>
  </p:identity>
</p:declare-step>

</p:library>
