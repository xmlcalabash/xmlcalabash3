<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="http://docs.xmlcalabash.com/ns/example"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                type="ex:add-status"
                version="3.0">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:option name="status" select="'draft'" as="xs:string"/>

  <p:if test="not(/*/@status)">
    <p:add-attribute attribute-name="status"
                     attribute-value="{$status}"/>
  </p:if>

</p:declare-step>
