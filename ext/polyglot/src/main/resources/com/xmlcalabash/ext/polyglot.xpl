<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           version="3.0">
  <p:declare-step type="cx:polyglot">
    <p:input port="source" primary="true" sequence="true">
      <p:empty/>
    </p:input>
    <p:input port="program" content-types="text"/>
    <p:output port="result"/>
    <p:option name="language" as="xs:string"/>
    <p:option name="args" as="xs:string*"/>
    <p:option name="result-content-type" as="xs:string?"/>
    <p:option name="parameters" as="map(xs:QName,item()?)?"/> 
  </p:declare-step>

  <p:declare-step type="cx:python">
    <p:input port="source" primary="true" sequence="true">
      <p:empty/>
    </p:input>
    <p:input port="program" content-types="text"/>
    <p:output port="result"/>
    <p:option name="args" as="xs:string*"/>
    <p:option name="result-content-type" as="xs:string?"/>
    <p:option name="parameters" as="map(xs:QName,item()?)?"/> 
  </p:declare-step>

  <p:declare-step type="cx:javascript">
    <p:input port="source" primary="true" sequence="true">
      <p:empty/>
    </p:input>
    <p:input port="program" content-types="text"/>
    <p:output port="result"/>
    <p:option name="args" as="xs:string*"/>
    <p:option name="result-content-type" as="xs:string?"/>
    <p:option name="parameters" as="map(xs:QName,item()?)?"/> 
  </p:declare-step>

  <p:declare-step type="cx:ruby">
    <p:input port="source" primary="true" sequence="true">
      <p:empty/>
    </p:input>
    <p:input port="program" content-types="text"/>
    <p:output port="result"/>
    <p:option name="args" as="xs:string*"/>
    <p:option name="result-content-type" as="xs:string?"/>
    <p:option name="parameters" as="map(xs:QName,item()?)?"/> 
  </p:declare-step>

  <p:declare-step type="cx:r">
    <p:input port="source" primary="true" sequence="true">
      <p:empty/>
    </p:input>
    <p:input port="program" content-types="text"/>
    <p:output port="result"/>
    <p:option name="args" as="xs:string*"/>
    <p:option name="result-content-type" as="xs:string?"/>
    <p:option name="parameters" as="map(xs:QName,item()?)?"/> 
  </p:declare-step>

  <p:declare-step type="cx:java">
    <p:input port="source" primary="true" sequence="true">
      <p:empty/>
    </p:input>
    <p:input port="program" content-types="text"/>
    <p:output port="result"/>
    <p:option name="args" as="xs:string*"/>
    <p:option name="result-content-type" as="xs:string?"/>
    <p:option name="parameters" as="map(xs:QName,item()?)?"/> 
  </p:declare-step>
</p:library>
