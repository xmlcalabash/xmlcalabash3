<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:s="http://purl.oclc.org/dsdl/schematron"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="#all" version="3.0">
<p:input port="source">
  <p:pipeinfo>
    <s:schema queryBinding="xslt2">
      <s:ns prefix="ex" uri="https://xmlcalabash.com/ns/examples"/>
      <s:pattern>
        <s:rule context="/">
          <s:report test="*/@xml:id">The source document has a root id.</s:report>
          <s:report test="not(*/@xml:id)">The source document does not have a root id.</s:report>
          <s:assert test="ex:book">The source is not a book.</s:assert>
        </s:rule>
      </s:pattern>
    </s:schema>
  </p:pipeinfo>
</p:input>
<p:output port="result">
  <p:pipeinfo>
    <s:schema queryBinding="xslt2">
      <s:pattern>
        <s:rule context="/">
          <s:assert test="string(/*/@unique-id) != ''">The output does not have a unique id.</s:assert>
        </s:rule>
      </s:pattern>
    </s:schema>
  </p:pipeinfo>
</p:output>

<p:add-attribute attribute-name="unique-id" attribute-value=""/>

<p:uuid match="/*/@unique-id"/>

</p:declare-step>
