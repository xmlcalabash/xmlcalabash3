<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:i="http://example.com/import/test"
           version="3.0">

<p:import href="B.xpl"/>
<p:import href="C.xpl"/>

<p:declare-step type="i:a">
  <p:output port="result" />

  <p:declare-step type="i:nested">
    <p:output port="result" />
    <p:identity>
      <p:with-input><doc>this is i:nested</doc></p:with-input>
    </p:identity>
  </p:declare-step>

  <p:identity>
    <p:with-input><doc>this is i:a</doc></p:with-input>
  </p:identity>
</p:declare-step>

<p:declare-step type="i:private" visibility="private">
  <p:output port="result" />
  <p:identity>
    <p:with-input><doc>this is i:private in A</doc></p:with-input>
  </p:identity>
</p:declare-step>


</p:library>
