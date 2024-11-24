<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:i="http://example.com/import/test"
           version="3.0">

<p:import href="A.xpl"/>

<p:declare-step type="i:b">
  <p:output port="result" />
  <p:identity>
    <p:with-input><doc>this is i:b</doc></p:with-input>
  </p:identity>
</p:declare-step>

<p:declare-step type="i:private" visibility="private">
  <p:import href="C.xpl" use-when="false()"/>
  <p:output port="result" />
  <p:identity>
    <p:with-input><doc>this is i:private in B</doc></p:with-input>
  </p:identity>
</p:declare-step>

</p:library>
