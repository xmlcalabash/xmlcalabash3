<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:i="http://example.com/import/test"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           version="3.1">

<p:option static="true" name="i:option" select="17" as="xs:integer"/>

<p:declare-step type="i:a">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:identity/>
</p:declare-step>

<p:declare-step type="i:private" visibility="private">
  <p:output port="result" />
  <p:identity>
    <p:with-input><doc>this is i:private in A</doc></p:with-input>
  </p:identity>
</p:declare-step>

</p:library>
