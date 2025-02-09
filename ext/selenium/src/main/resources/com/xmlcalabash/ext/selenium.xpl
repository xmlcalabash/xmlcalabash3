<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:selenium">
  <p:input port="source" content-types="text"/>
  <p:output port="result" sequence="true"/>
  <p:option name="browser" as="xs:string?"/>
  <p:option name="capabilities" as="map(xs:QName, item())?"/>
  <p:option name="arguments" as="xs:string*"/>
</p:declare-step>

</p:library>
