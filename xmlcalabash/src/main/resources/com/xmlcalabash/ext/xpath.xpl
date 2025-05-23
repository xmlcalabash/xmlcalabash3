<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:xpath">
  <p:input port="source" content-types="any" sequence="true" primary="true"/>
  <p:input port="xpath" content-types="text"/>
  <p:output port="result" sequence="true" content-types="any"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>     
  <p:option name="version" as="xs:string?"/> 
</p:declare-step>

</p:library>
