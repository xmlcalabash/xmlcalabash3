<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="3.0">
  <p:import href="https://xmlcalabash.com/ext/library/diagramming.xpl"/>

  <cx:mathml-to-svg parameters="map {'mathsize': '30f'}">
    <p:with-input href="../examples/xml/det-a.xml"/>
  </cx:mathml-to-svg>

  <p:store name="store" href="det-a.svg"/>
</p:declare-step>
