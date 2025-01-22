<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="3.0">
  <p:import href="https://xmlcalabash.com/ext/library/diagramming.xpl"/>

  <cx:plantuml>
    <p:with-input href="../txt/plantuml-003.txt"/>
  </cx:plantuml>

  <p:store name="store" href="plantuml-003.png"/>
</p:declare-step>
