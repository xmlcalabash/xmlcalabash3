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

<cx:polyglot language="python"
             parameters="map {'x': 17,
                              'cx:s': 'String',
                              'doc': $doc,
                              'map': map { 'a': 1 },
                              'a': [ map{'b': 2}, map{'c': 3}] }">
  <p:with-option name="args" select="('a','b','c')"/>
  <p:with-input port="source">
    <p:inline>{$doc}</p:inline>
  </p:with-input>
  <p:with-input port="program">
    <p:inline content-type="text/plain" expand-text="false">
import sys
print(sys.argv)
for line in sys.stdin:
    print(line.rstrip())
x + len(doc)
    </p:inline>
  </p:with-input>
</cx:polyglot>

</p:declare-step>
