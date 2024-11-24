<p:library version="3.0"
           xmlns:p="http://www.w3.org/ns/xproc">
  
<p:declare-step version="3.0"
                type="test:stepD"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:test="http://test">
  <p:import href="import4.xpl"/>
  <p:output port="result" />
  <p:identity>
    <p:with-input><success /></p:with-input>
  </p:identity>
</p:declare-step>

</p:library>
