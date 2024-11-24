<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:test="http://example.com/ns/test"
                version="3.1">
<p:output port="result" sequence="true"/>

<p:declare-step type="test:yes" use-when="p:step-available('test:no')">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:identity/>
</p:declare-step>

<p:declare-step type="test:no" use-when="p:step-available('test:yes')">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:identity/>
</p:declare-step>

<test:yes/>

</p:declare-step>
