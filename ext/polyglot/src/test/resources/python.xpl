<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/polyglot.xpl"/>
<p:output port="result"/>

<cx:python parameters="map{'x': 7, 'y': 9, 'now': current-dateTime()}">
  <p:with-input>
    <doc>Test</doc>
  </p:with-input>
  <p:with-option name="args" select="('a', 'b', 'c')"/>
  <p:with-input port="program">
    <p:inline content-type="text/plain" expand-text="false">
import sys
print(f"Program arguments: {sys.argv}")
print("Program input:")
for line in sys.stdin:
    print(line.rstrip())
print(f"x*y = {x*y}")
print("Now:")
print(now)

"The last expression is returned"
    </p:inline>
  </p:with-input>
</cx:python>

</p:declare-step>
