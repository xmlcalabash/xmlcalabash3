<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:test="http://example.com/ns/test"
                xmlns:i="http://example.com/import/test"
                name="main" version="3.0">
<p:output port="result" sequence="true"/>

<!-- This test will only succeed if the a compile-time value
     of 12 is given for the static option $i:option -->

<p:import href="A.xpl"/>

<p:identity>
  <p:with-input use-when="$i:option = 12"><yes/></p:with-input>
  <p:with-input use-when="$i:option = 17"><no/></p:with-input>
</p:identity>

</p:declare-step>
