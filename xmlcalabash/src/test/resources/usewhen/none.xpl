<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:ex="http://example.com/ns/xproc-extensions"
                exclude-inline-prefixes="#all"
                xmlns:test="http://example.com/ns/test"
                name="main" type="ex:spoon" version="3.0">
<p:output port="result" sequence="true"/>

<p:identity>
  <p:with-input><yes/></p:with-input>
</p:identity>

</p:declare-step>
