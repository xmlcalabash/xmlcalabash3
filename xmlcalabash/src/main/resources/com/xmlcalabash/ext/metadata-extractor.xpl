<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">
  <p:declare-step type="cx:metadata-extractor">
    <p:input port="source" content-types="any"/>
    <p:output port="result" content-types="xml" sequence="true"/>
    <p:option name="assert-metadata" as="xs:boolean" select="false()"/>
    <p:option name="properties" as="map(xs:QName,item()*)?"/>
  </p:declare-step>
</p:library>
