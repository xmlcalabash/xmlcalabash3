<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">

<p:declare-step type="cx:xmlunit">
  <p:input port="source" content-types="xml html"/>
  <p:input port="alternate" content-types="xml html"/>
  <p:output port="result" primary="true"/>
  <p:output port="report" sequence="true"/>
  <p:option name="check-for" as="xs:string?" values="('identity', 'similarity')"/>
  <p:option name="element-selector" as="xs:string?" select="()"
            values="('by-name', 'by-name-and-text', 'by-name-and-all-attributes',
                     'by-name-and-attributes')"/>
  <p:option name="attribute-list" as="xs:QName*"/>
  <p:option name="report-format" select="'xvrl'" as="xs:string"/>
  <p:option name="ignore-comments" select="false()" as="xs:boolean"/>
  <p:option name="ignore-whitespace" select="false()" as="xs:boolean"/>
  <p:option name="normalize-whitespace" select="false()" as="xs:boolean"/>  
  <p:option name="fail-if-not-equal" as="xs:boolean" select="true()"/>

  <p:option name="node-matcher-class" as="xs:string?"/>
  <p:option name="element-selector-class" as="xs:string?"/>
</p:declare-step>

</p:library>
