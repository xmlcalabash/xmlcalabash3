<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:ebnf-convert">
  <p:input port="source" content-types="text"/>
  <p:output port="result" content-types="xml text"/>
  <p:option name="factoring" as="xs:string" select="'full-left'"
            values="('full-left', 'left-only', 'full-right', 'right-only', 'none')"/>
  <p:option name="remove-recursion" as="xs:string" select="'full'"
            values="('full', 'left', 'right', 'none')"/>
  <p:option name="inline-terminals" as="xs:boolean" select="true()"/>
  <p:option name="epsilon-references" as="xs:boolean" select="false()"/>
  <p:option name="notation" as="xs:string?"
            values="('abnf', 'antlr_3', 'antlr_4', 'bison', 'gold', 'instaparse', 'ixml',
                     'javacc', 'jison', 'pegjs', 'phythia', 'pss', 'rex_5_9', 'w3c', 'xtext')"/>
  <p:option name="xml" as="xs:boolean" select="false()"/>
  <p:option name="verbose" as="xs:boolean" select="false()"/>
  <p:option name="timestamp" as="xs:boolean" select="true()"/>
</p:declare-step>

</p:library>
