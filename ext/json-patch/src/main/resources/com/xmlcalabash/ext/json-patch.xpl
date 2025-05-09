<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:json-diff">
  <p:input port="source" content-types="json" primary="true"/>
  <p:input port="target" content-types="json"/>
  <p:output port="result" content-types="json"/>
</p:declare-step>

<p:declare-step type="cx:json-patch">
  <p:input port="source" content-types="json" primary="true"/>
  <p:input port="patch" content-types="json"/>
  <p:output port="result" content-types="json"/>
  <p:option name="merge" as="xs:boolean" select="false()"/>
</p:declare-step>

</p:library>
