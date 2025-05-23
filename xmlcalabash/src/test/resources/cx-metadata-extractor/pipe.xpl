<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                exclude-inline-prefixes="cx xs"
                version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/metadata-extractor.xpl"/>

<p:output port="result"/>
<p:option name="href" required="true"/>

<cx:metadata-extractor>
  <p:with-input>
    <p:document href="{$href}"/>
  </p:with-input>
</cx:metadata-extractor>

</p:declare-step>
