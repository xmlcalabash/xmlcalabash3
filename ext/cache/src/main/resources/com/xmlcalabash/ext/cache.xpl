<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

  <p:declare-step type="cx:cache-add">
    <p:option name="href" as="xs:anyURI?" select="()"/>
    <p:option name="fail-if-in-cache" as="xs:boolean" select="false()"/>
    <p:input port="source"/>
    <p:output port="result"/>
  </p:declare-step>

  <p:declare-step type="cx:cache-delete">
    <p:option name="href" as="xs:anyURI?" select="()"/>
    <p:option name="fail-if-not-in-cache" as="xs:boolean" select="false()"/>
    <p:option name="content-type" as="xs:string" select="'*/*'"/>
    <p:input port="source"/>
    <p:output port="result"/>
  </p:declare-step>

</p:library>
