<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

  <p:declare-step type="cx:rdf-merge">
    <p:input port="source" sequence="true"/>
    <p:option name="graph" as="xs:anyURI?"/>
    <p:output port="result"/>
  </p:declare-step>

  <p:declare-step type="cx:rdf-graph">
    <p:input port="source"/>
    <p:output port="result"/>
    <p:option name="graph" as="xs:anyURI?"/>
  </p:declare-step>

  <p:declare-step type="cx:sparql">
    <p:input port="source"/>
    <p:input port="query" content-types="text"/>
    <p:output port="result"/>
    <p:option name="content-type" as="xs:string?"/>
  </p:declare-step>

  <p:declare-step type="cx:rdfa" xmlns:cx="http://xmlcalabash.com/ns/extensions">
    <p:input port="source" content-types="xml html"/>
    <p:output port="result" content-types="application/rdf+thrift"/>
  </p:declare-step>

</p:library>
