<p:declare-step version="3.0"
                xmlns:err="http://www.w3.org/ns/xproc-error"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:s="http://purl.oclc.org/dsdl/schematron"
                xmlns:p="http://www.w3.org/ns/xproc">
  <p:import href="https://xmlcalabash.com/ext/library/pipeline-messages.xpl"/>
  <p:input port="source">
    <p:pipeinfo>
      <s:schema queryBinding="xslt2">
        <s:pattern>
          <s:rule context="/">
            <s:report test="doc">The source document is a doc</s:report>
            <s:assert test="doc">The source document is not a doc</s:assert>
          </s:rule>
        </s:pattern>
      </s:schema>
    </p:pipeinfo>
  </p:input>
  <p:output port="result"/>
  <p:identity name="identity"/>
  <cx:pipeline-messages p:depends="identity" level="info" clear="true"/>
</p:declare-step>
