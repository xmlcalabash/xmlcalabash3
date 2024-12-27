<p:declare-step version="3.0"
                xmlns:err="http://www.w3.org/ns/xproc-error"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:s="http://purl.oclc.org/dsdl/schematron"
                xmlns:p="http://www.w3.org/ns/xproc">
  <p:import href="https://xmlcalabash.com/ext/library/pipeline-messages.xpl"/>
  <p:input port="source" cx:assertions="'input-correct'"/>
  <p:output port="result" cx:assertions="'output-correct'"/>
  <p:identity name="identity"/>
  <cx:pipeline-messages p:depends="identity" level="info" clear="true"/>

  <p:pipeinfo>
    <s:schema queryBinding="xslt2" xml:id="input-correct">
      <s:pattern>
        <s:rule context="/">
          <s:report test="doc">The source document is a doc</s:report>
          <s:assert test="doc">The source document is not a doc</s:assert>
        </s:rule>
      </s:pattern>
    </s:schema>

    <s:schema queryBinding="xslt2" xml:id="output-correct">
      <s:pattern>
        <s:rule context="/">
          <s:report test="doc">The result document is a doc</s:report>
          <s:assert test="doc">The result document is not a doc</s:assert>
        </s:rule>
      </s:pattern>
    </s:schema>
  </p:pipeinfo>
</p:declare-step>
