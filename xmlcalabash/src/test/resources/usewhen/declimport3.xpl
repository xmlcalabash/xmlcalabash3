<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:test="http://example.com/ns/test"
                xmlns:i="http://example.com/import/test"
                version="3.1">
<p:output port="result" sequence="true"/>

<p:import href="B.xpl" use-when="p:step-available('test:yes')"/>

<p:declare-step type="test:yes" use-when="true()">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:identity/>
</p:declare-step>

<p:identity>
  <p:with-input use-when="p:step-available('i:a')"><yes/></p:with-input>
  <p:with-input use-when="not(p:step-available('i:a'))"><no/></p:with-input>
</p:identity>

</p:declare-step>
