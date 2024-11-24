<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:i="http://example.com/import/test"
           version="3.1">

<p:import href="A.xpl"/>

<p:declare-step type="i:b">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:identity/>
</p:declare-step>

</p:library>
