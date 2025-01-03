<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/polyglot.xpl"/>

<p:output port="result"/>

<p:variable name="doc" select="/">
  <p:inline>
    <doc><p>text</p></doc>
  </p:inline>
</p:variable>

<cx:r>
  <p:with-input port="source"><p:empty/></p:with-input>
  <p:with-input port="program">
    <p:inline content-type="text/plain" expand-text="false">
print("Hello World!", quote = FALSE)
</p:inline>
  </p:with-input>
</cx:r>

</p:declare-step>
