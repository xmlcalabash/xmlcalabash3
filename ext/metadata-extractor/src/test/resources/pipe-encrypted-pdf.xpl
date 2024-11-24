<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                exclude-inline-prefixes="cx xs"
                version="3.0">

<p:output port="result"/>

<p:declare-step type="cx:metadata-extractor">
  <p:input port="source" content-types="any"/>
  <p:output port="result" content-types="xml" sequence="true"/>
  <p:option name="assert-metadata" as="xs:boolean" select="false()"/>
  <p:option name="properties" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<cx:metadata-extractor properties="map { 'password': 'this is sekrit' }">
  <p:with-input>
    <p:document href="document.pdf"/>
  </p:with-input>
</cx:metadata-extractor>

</p:declare-step>
