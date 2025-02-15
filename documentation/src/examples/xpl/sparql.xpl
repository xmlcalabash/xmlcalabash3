<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                version="3.0">
<p:import href="https://xmlcalabash.com/ext/library/rdf.xpl"/>
<p:input port="source" primary="true"/>
<p:output port="result"/>

<cx:sparql>
  <p:with-input port="query" href="../rdf/query.rq"/>
</cx:sparql>

<p:cast-content-type content-type="text/plain"/>

</p:declare-step>
