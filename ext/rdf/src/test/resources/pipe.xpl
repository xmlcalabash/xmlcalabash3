<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:output port="result"/>

<p:declare-step type="cx:unique-id">
     <p:input port="source" primary="true" content-types="xml html"/>
     <p:output port="result" content-types="text xml html"/>
     <p:option name="match" as="xs:string" select="'/*'"/>  <!-- XSLTSelectionPattern -->
     <p:option name="flavor" as="xs:string" select='uuid' values="('uuid','ulid')"/>
     <p:option name="sequential" as="xs:boolean" select='false()'/>
     <p:option name="parameters" as="map(xs:QName,item()*)?"/> 
</p:declare-step>

<cx:unique-id match="/doc/uuid/text()" flavor="uuid">
  <p:with-input>
    <doc>
      <uuid>x</uuid>
      <uuid>x</uuid>
      <ulid>x</ulid>
      <ulid>x</ulid>
      <sequuid>x</sequuid>
      <sequuid>x</sequuid>
      <sequuid>x</sequuid>
      <sequlid>x</sequlid>
      <sequlid>x</sequlid>
      <sequlid>x</sequlid>
    </doc>
  </p:with-input>
</cx:unique-id>

<cx:unique-id match="/doc/ulid/text()" flavor="ulid"/>
<cx:unique-id match="/doc/sequuid/text()" flavor="uuid" sequential="true"/>
<cx:unique-id match="/doc/sequlid/text()" flavor="ulid" sequential="true"/>

</p:declare-step>
