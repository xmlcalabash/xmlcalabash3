<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:i="http://example.com/import/test"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           version="3.1">

<p:option static="true" name="i:option" select="17" as="xs:integer"/>

<p:declare-step type="i:step">
  <p:input port="source"/>
  <p:output port="result"/>

  <p:variable name="i:x" select="/">
    <p:inline exclude-inline-prefixes="i">
      <yes p:use-when="true()"/>
      <no p:use-when="false()"/>
    </p:inline>
  </p:variable>

  <p:identity/>
</p:declare-step>

</p:library>
