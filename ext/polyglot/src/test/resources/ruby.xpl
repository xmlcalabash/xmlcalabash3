<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/polyglot.xpl"/>

<p:output port="result"/>

<cx:ruby parameters="map {'x': 17 }">
  <p:with-input port="source">
    <p:empty/>
  </p:with-input>
  <p:with-input port="program">
    <p:inline content-type="text/plain" expand-text="false">
x + 4
    </p:inline>
  </p:with-input>
</cx:ruby>

</p:declare-step>
