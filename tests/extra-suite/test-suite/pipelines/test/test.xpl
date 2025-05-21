<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step" 
                xmlns:cx="http://xmlcalabash.com/ns/extensions" 
                xmlns:ex="http://example.com/xproc"
                version="3.0" exclude-inline-prefixes="#all">
    <p:import href="../step.xpl"/>
    
    <p:input port="source" sequence="true"/>
    <p:output port="result" primary="true" sequence="true"/>
    
    <!-- test --> 
    <ex:step name="test">
      <p:with-option name="test" select="resolve-uri('test.xml')"></p:with-option>
    </ex:step>
</p:declare-step>
