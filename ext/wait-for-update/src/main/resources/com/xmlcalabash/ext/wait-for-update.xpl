<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:wait-for-update">
  <p:output port="result"/>
  <p:option name="href" as="xs:anyURI" required="true"/>
  <p:option name="pause" as="xs:string" select="'PT1S'"/>
  <p:option name="pause-after" as="xs:string" select="'0'"/>
</p:declare-step>

</p:library>
