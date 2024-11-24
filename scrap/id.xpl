<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                version="3.0">
<p:input port="source"/>
<p:output port="result"/>
<p:option name="spoon" required="true"/>
<p:identity>
  <p:with-input>
    <doc>TEST: {$spoon}</doc>
  </p:with-input>
</p:identity>
</p:declare-step>
