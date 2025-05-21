<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                name="step" type="ex:step"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ex="http://example.com/xproc"
                version="3.0" exclude-inline-prefixes="#all">
    
    <p:input port="source" sequence="true"></p:input>
    <p:output port="result" sequence="true"></p:output>
    
    <p:option name="test" select="''"/>
    <p:xslt template-name="main" parameters="map{'test':$test}">
      <p:with-input port="stylesheet" href="show.xsl"/>
    </p:xslt>
</p:declare-step>
