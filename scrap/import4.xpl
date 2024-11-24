<p:library version="3.0"
           xmlns:p="http://www.w3.org/ns/xproc">
  
<p:declare-step version="3.0"
                type="test:stepC"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:test="http://test">
  <p:import href="import5.xpl"/>
  <p:output port="result" />
  <test:stepD/>
</p:declare-step>

</p:library>
