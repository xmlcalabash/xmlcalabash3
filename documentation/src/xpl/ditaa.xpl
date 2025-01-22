<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="3.0">
  <p:import href="https://xmlcalabash.com/ext/library/diagramming.xpl"/>

  <cx:ditaa>
    <p:with-input href="../txt/ditaa.txt"/>
  </cx:ditaa>

  <p:store name="store" href="ditaa.png"/>
</p:declare-step>
