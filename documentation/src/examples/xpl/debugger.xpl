<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="https://xmlcalabash.com/ns/examples"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                version="3.0">
<p:output port="result" sequence="true"/>

<p:declare-step type="ex:ident">
  <p:input port="source" sequence="true"/>
  <p:output port="result" sequence="true"/>
  <p:identity/>
</p:declare-step>

<p:xinclude>
  <p:with-input href="../xml/default-input.xml"/>
</p:xinclude>

<p:identity name="id1"/>

<ex:ident name="ex1"/>

<p:add-attribute name="add" match="//ex:chap|//ex:app"
                 attribute-name="role" attribute-value="test"/>

<p:identity name="id2"/>

</p:declare-step>
