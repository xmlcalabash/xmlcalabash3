<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">
  <p:declare-step type="cx:trang">
    <p:input port="source" sequence="true" content-types="text xml application/xml-dtd"/>
    <p:output port="result" sequence="true" content-types="text xml application/xml-dtd"/>
    <p:option name="source-format" as="xs:string?" values="('rng', 'rnc', 'dtd')"/>
    <p:option name="result-format" as="xs:string" values="('rng', 'rnc', 'dtd', 'xsd')"
              required="true"/>
    <p:option name="output-base-uri" as="xs:anyURI" required="true"/>
    <p:option name="namespaces" as="map(xs:string, xs:string)?"/>
    <p:option name="source-parameters" as="map(xs:QName, item()*)?"/>
    <p:option name="result-parameters" as="map(xs:QName, item()*)?"/>
  </p:declare-step>

  <p:declare-step type="cx:trang-files">
    <p:output port="result" sequence="true" content-types="xml"/>
    <p:option name="source-schema" as="xs:anyURI" required="true"/>
    <p:option name="result-schema" as="xs:anyURI" required="true"/>
    <p:option name="source-format" as="xs:string?" values="('rng', 'rnc', 'dtd')"/>
    <p:option name="result-format" as="xs:string?" values="('rng', 'rnc', 'dtd', 'xsd')"/>
    <p:option name="namespaces" as="map(xs:string, xs:string)?"/>
    <p:option name="source-parameters" as="map(xs:QName, item()*)?"/>
    <p:option name="result-parameters" as="map(xs:QName, item()*)?"/>
  </p:declare-step>

</p:library>
