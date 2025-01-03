<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/polyglot.xpl"/>

<p:output port="result"/>

<cx:javascript parameters="map { 'x': 17, 's': 'String', 'map': map { 'a': 1 },
                                 'a': [ map{'b': 2}, map{'c': 3}] }">
  <p:with-input port="program">
    <p:inline content-type="text/plain" expand-text="false">
      function f(x) { 
        let date1 = new Date(1999, 7, 5)
        let date2 = new Date(2012, 1, 3, 12, 11, 14)
        return 12.3
      }
      f(x)
    </p:inline>
  </p:with-input>
</cx:javascript>

</p:declare-step>
