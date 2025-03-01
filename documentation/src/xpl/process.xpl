<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:db="http://docbook.org/ns/docbook"
                exclude-inline-prefixes="cx db"
                name="main" version="3.0">
  <p:import href="https://xmlcalabash.com/ext/library/railroad.xpl"/>
  <p:input port="source"/>
  <p:output port="result"/>

  <p:viewport match="db:programlisting[contains-token(@role, 'railroad')]">
    <cx:railroad>
      <p:with-option name="width"
                     select="if (tokenize(/*/@role, ' ')[last()] castable as xs:integer)
                             then xs:integer(tokenize(/*/@role, ' ')[last()])
                             else 992"/>
      <p:with-option name="nonterminal" select="/*/@wordsize"/>
      <p:with-option name="transform-links" select="&quot;'#fig.'||$p:nonterminal||'.railroad'&quot;"/>
      <p:with-input select="/*/text()"/>
    </cx:railroad>

    <p:identity>
      <p:with-input>
        <mediaobject xmlns="http://docbook.org/ns/docbook">
          <imageobject>
            <imagedata>{.}</imagedata>
          </imageobject>
        </mediaobject>
      </p:with-input>
    </p:identity>
  </p:viewport>

</p:declare-step>
