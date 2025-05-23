<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:collection-manager">
  <p:input port="source" sequence="true"/>
  <p:output port="result" sequence="true"/>
  <p:option name="source" as="xs:anyURI" required="true"/>
  <p:option name="stable" as="xs:boolean" select="true()"/>
</p:declare-step>

</p:library>
