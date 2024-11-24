<p:declare-step version="3.1"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="https://xmlcalabash.com/ns/examples">
  <p:input port="source"/>
  <p:output port="result"/>

  <p:delete match="//ex:chap/@class"/>

</p:declare-step>
