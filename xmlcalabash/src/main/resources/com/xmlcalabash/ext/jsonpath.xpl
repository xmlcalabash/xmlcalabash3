<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:jsonpath">
  <p:input port="source" content-types="json"/>
  <p:output port="result" content-types="json"/>
  <p:option name="query" as="xs:string" required="true"/>
  <p:option name="parameters" as="map(xs:QName, item()*)?"/>
</p:declare-step>

</p:library>
