<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main" version="3.0">
  <p:import href="https://xmlcalabash.com/ext/library/railroad.xpl"/>
  <p:output port="result" sequence="true"/>

  <cx:railroad>
    <p:with-input>
      <p:inline content-type="text/plain">
Expression ::= Number op Number | "(" Expression ")"
Number ::= [0-9]+
op ::= "+" | "-" | "×" | "÷"
      </p:inline>
    </p:with-input>
  </cx:railroad>
</p:declare-step>
