<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:test="http://example.com/ns/test"
                name="main" version="3.0">
<p:output port="result" sequence="true"/>

<p:option static="true" name="test:prime" select="17"
          use-when="p:step-available('test:step')"/>

<p:declare-step type="test:step"
                use-when="not(p:step-available('test:not-a-step'))">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:identity/>
</p:declare-step>

<p:identity>
  <p:with-input use-when="$test:prime mod 2 = 0"><no/></p:with-input>
  <p:with-input use-when="$test:prime mod 2 = 1"><yes/></p:with-input>
</p:identity>

</p:declare-step>
