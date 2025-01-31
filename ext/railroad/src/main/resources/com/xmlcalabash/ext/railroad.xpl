<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:railroad">
  <p:input port="source" content-types="text"/>
  <p:output port="result" content-types="image/svg+xml" primary="true" sequence="true"/>
  <p:output port="html" content-types="html"/>
  <p:option name="nonterminal" as="xs:string?"/>
  <p:option name="color" as="xs:string?" select="'#FFDB4D'"/>
  <p:option name="color-offset" as="xs:integer" select="0"/>
  <p:option name="padding" as="xs:integer?"/>
  <p:option name="stroke-width" as="xs:integer?"/>
  <p:option name="width" as="xs:integer" select="992"/>
  <p:option name="eliminate-recursion" as="xs:boolean" select="true()"/>
  <p:option name="factoring" as="xs:boolean" select="true()"/>
  <p:option name="inline-literals" as="xs:boolean" select="true()"/>
  <p:option name="keep-epsilon-nonterminals" as="xs:boolean" select="true()"/>
</p:declare-step>

</p:library>
