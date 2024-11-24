<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:output port="result"/>

<!--
<p:declare-step type="cx:invisible-xml">
     <p:input port="grammar" sequence="true" content-types="any"/>
     <p:input port="source" primary="true" content-types="any"/>
     <p:output port="result" content-types="any"/>
     <p:option name="parameters" as="map(xs:QName, item()*)?"/>    
     <p:option name="fail-on-error" as="xs:boolean" select="true()"/>
</p:declare-step>
-->

<p:invisible-xml>
  <p:with-input port="grammar">
    <p:document href="date.ixml" content-type="text/plain"/>
  </p:with-input>
  <p:with-input port="source">
    <p:inline content-type="text/plain">4 March 2022</p:inline>
  </p:with-input>
</p:invisible-xml>

</p:declare-step>
