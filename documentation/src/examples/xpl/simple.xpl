<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                version="3.0">
<p:output port="result" sequence="true"/>

<p:xinclude>
  <p:with-input href="../xml/default-input.xml"/>
</p:xinclude>

<p:add-attribute name="add" match="/*/*"
                 attribute-name="role" attribute-value="test"/>

</p:declare-step>
