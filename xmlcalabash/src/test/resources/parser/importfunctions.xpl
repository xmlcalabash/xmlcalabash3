<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:i="http://example.com/import/test"
           xmlns:f="https://xmlcalabash.com/ns/functions/xsl"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           expand-text="true" exclude-inline-prefixes="#all"
           version="3.1">

<p:import-functions href="functions.xsl"/>
<p:import-functions href="functions.xqy"
                    namespace="https://xmlcalabash.com/ns/functions/xqy"/>

<p:declare-step type="i:step">
  <p:output port="result"/>
  <p:identity>
    <p:with-input>
      <p:inline><doc hello="{f:hello()}"/></p:inline>
    </p:with-input>
  </p:identity>
</p:declare-step>

</p:library>
