<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                version="3.0">
<p:import href="https://xmlcalabash.com/ext/library/rdf.xpl"/>
<p:input port="source"/>
<p:output port="result"/>

<!-- The base URI from the documention build system is distracting -->
<p:set-properties properties="map{'base-uri': 'http://example.org/rdfa.html'}"/>

<cx:rdfa/>

<p:cast-content-type content-type="text/turtle"/>

</p:declare-step>
