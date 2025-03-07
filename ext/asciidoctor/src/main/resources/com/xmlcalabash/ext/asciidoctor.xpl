<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:asciidoctor">
  <p:input port="source" content-types="text/asciidoc text"/>
  <p:output port="result" content-types="xml html application/pdf"/>
  <p:option name="backend" as="xs:string?" values="('docbook', 'html5', 'pdf')"/>
  <p:option name="parameters" as="map(xs:QName, item()*)?"/>
  <p:option name="attributes" as="map(xs:QName, item()*)?"/>
</p:declare-step>

</p:library>
