<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:s="http://purl.oclc.org/dsdl/schematron"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="#all" version="3.0">
<p:input port="source" cx:assertions="'assert-input'"/>
<p:output port="result"/>

<p:add-attribute attribute-name="unique-id" attribute-value=""/>

<p:uuid match="/*/@unique-id"
        cx:assertions="map { 'result': 'assert-output' }">
  <p:with-input cx:assertions="'assert-attribute'"/>
</p:uuid>

<!-- ============================================================ -->

<p:pipeinfo>
  <s:schema queryBinding="xslt2" xml:id="assert-input">
    <s:ns prefix="ex" uri="https://xmlcalabash.com/ns/examples"/>
    <s:pattern>
      <s:rule context="/">
        <s:report test="*/@xml:id">The source document has a root id.</s:report>
        <s:report test="not(*/@xml:id)">The source document does not have a root id.</s:report>
        <s:assert test="ex:book">The source is not a book.</s:assert>
      </s:rule>
    </s:pattern>
  </s:schema>

  <s:schema queryBinding="xslt2" xml:id="assert-output">
    <s:pattern>
      <s:rule context="/">
        <s:assert test="string(/*/@unique-id) != ''">The output does not have a unique id.</s:assert>
      </s:rule>
    </s:pattern>
  </s:schema>

  <s:schema queryBinding="xslt2" xml:id="assert-attribute">
    <s:pattern>
      <s:rule context="/">
        <s:assert test="*/@unique-id">The input does not have a unique-id attribute.</s:assert>
      </s:rule>
    </s:pattern>
  </s:schema>
</p:pipeinfo>

</p:declare-step>
