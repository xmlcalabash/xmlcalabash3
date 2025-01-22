<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:ditaa">
  <p:input port="source" content-types="text"/>
  <p:output port="result"/>
  <p:option name="content-type" as="xs:string?" select="'image/png'"/>
  <p:option name="parameters" as="map(xs:QName, item()*)?"/>
</p:declare-step>

<p:declare-step type="cx:mathml-to-svg">
  <p:input port="source" content-types="xml"/>
  <p:output port="result"/>
  <p:option name="parameters" as="map(xs:QName, item()*)?"/>
</p:declare-step>

<p:declare-step type="cx:plantuml">
  <p:input port="source" content-types="text"/>
  <p:output port="result"/>
  <p:option name="format" as="xs:string?" select="'png'"/>
  <p:option name="parameters" as="map(xs:QName, item()*)?"/>
</p:declare-step>

</p:library>
