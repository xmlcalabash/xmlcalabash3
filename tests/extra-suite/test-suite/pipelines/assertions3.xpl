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

  <p:pipeinfo cx:href="../documents/assertions.xml"/>
</p:declare-step>
