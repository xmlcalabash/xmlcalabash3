<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                exclude-inline-prefixes="#all"
                version="3.0">
  <p:output port="result"/>
  
  <p:identity>
    <p:with-input>
      <helloWorld>This is {p:system-property('p:product-name')
      } version {p:system-property('p:product-version')}.
Share and enjoy!</helloWorld>
    </p:with-input>
  </p:identity>

</p:declare-step>
