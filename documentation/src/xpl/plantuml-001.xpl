<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="3.0">
  <p:import href="https://xmlcalabash.com/ext/library/diagramming.xpl"/>

  <cx:plantuml>
    <p:with-input href="../txt/plantuml-001.txt"/>
  </cx:plantuml>

  <p:store name="store" href="plantuml-001.png"/>
</p:declare-step>
