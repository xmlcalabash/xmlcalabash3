<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:test="http://example.com/"
                name="main" version="3.0">
  <p:input port="source"/>
  <p:output port="result"/>

  <p:add-attribute attribute-name="extension-function"
                   attribute-value="{test:test()}"/>

</p:declare-step>
