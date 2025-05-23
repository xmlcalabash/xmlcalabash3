<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.1">

<p:declare-step type="cx:find">
  <p:output port="result" content-types="application/xml"/>
  <p:option name="path" required="true" as="xs:anyURI"/>        
  <p:option name="detailed" as="xs:boolean" select="false()"/>  
  <p:option name="max-depth" as="xs:string?" select="'unbounded'"/>     
  <p:option name="include-filter" as="xs:string*"/>             
  <p:option name="exclude-filter" as="xs:string*"/>             
  <p:option name="override-content-types" as="array(array(xs:string))?"/>
  <p:option name="xpath" as="xs:string*"/>
  <p:option name="grep" as="xs:string*"/>
  <p:option name="jsonpath" as="xs:string*"/>
</p:declare-step>

</p:library>
