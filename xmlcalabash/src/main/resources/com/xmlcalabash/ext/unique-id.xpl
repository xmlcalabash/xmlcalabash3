<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">
  <p:declare-step type="cx:unique-id">
    <p:input port="source" primary="true" content-types="xml html"/>
    <p:output port="result" content-types="text xml html"/>
    <p:option name="match" as="xs:string" select="'/*'"
              e:type="XSLTSelectionPattern"/>
    <p:option name="flavor" as="xs:string" select="'uuid'"
              values="('uuid', 'ulid', 'typeid')"/>
    <p:option name="sequential" as="xs:boolean" select="false()"/>
    <p:option name="parameters" as="map(xs:QName,item()*)?"/> 
  </p:declare-step>
</p:library>
