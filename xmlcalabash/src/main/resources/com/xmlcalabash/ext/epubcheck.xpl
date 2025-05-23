<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:epubcheck">
  <p:input port="source" content-types="application/zip application/epub+zip"/>
  <p:output port="result" content-types="application/zip application/epub+zip" primary="true"/>
  <p:output port="report" sequence="true" content-types="xml json"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
  <p:option name="assert-valid" select="true()" as="xs:boolean"/>
  <p:option name="report-format" select="'xvrl'" as="xs:string"/>
</p:declare-step>

</p:library>
