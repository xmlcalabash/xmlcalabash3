<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:pipeline-messages">
  <p:option name="level" as="xs:string?" select="()"/>
  <p:option name="clear" as="xs:boolean" select="false()"/>
  <p:output port="result"/>
</p:declare-step>

</p:library>
