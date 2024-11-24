<p:declare-step version="3.1"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="https://xmlcalabash.com/ns/examples">
  <p:input port="source"/>
  <p:output port="result"/>

  <p:add-attribute match="//ex:doc"
                   attribute-name="status"
                   attribute-value="draft"/>

  <p:add-attribute match="//ex:chap"
                   attribute-name="class"
                   attribute-value="chapter"/>
</p:declare-step>
