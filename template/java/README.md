# Java step template

This is a template for writing an XML Calabash 3.x extension step in Java.

```
<p:declare-step type="cx:template-java">
  <p:input port="source" content-types="any" sequence="true"/>
  <p:output port="result" content-types="xml"/>
  <p:option name="option" as="xs:boolean" select="false()"/>
</p:declare-step>
```

It simply reports the number of XML, text, JSON, and non-XML documents
passed to it.
