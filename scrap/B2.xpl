<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:i="http://example.com/import/test"
           version="3.0">

<p:declare-step type="i:b">
  <p:output port="result" />
  <p:identity>
    <p:with-input><doc>this is a duplicate i:b</doc></p:with-input>
  </p:identity>
</p:declare-step>

</p:library>
