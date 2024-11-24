<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:output port="result"/>

<p:declare-step type="cx:template-kotlin">
  <p:input port="source" content-types="any" sequence="true"/>
  <p:output port="result" content-types="xml"/>
  <p:option name="option" as="xs:boolean" select="false()"/>
</p:declare-step>

<cx:template-kotlin>
  <p:with-input>
    <p:document href="doc.json"/>
    <p:document href="scalar.json"/>
    <p:document href="../../../README.md"/>
    <p:document href="pipe.xpl"/>
  </p:with-input>
</cx:template-kotlin>

</p:declare-step>
