<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                version="3.0">

  <p:output port="result"/>

  <p:identity name="example">
    <p:with-input>
      <p:inline document-properties="map {
                  'base-uri': 'file:///path/to/examples/' }">
        <example/>
      </p:inline>
    </p:with-input>
  </p:identity>

  <p:add-xml-base name="fixup"/>

  <p:insert position="first-child" match="/wrapper/without-fixup">
    <p:with-input port="source">
      <wrapper xml:base="http://example.com/">
        <without-fixup/>
        <with-add-xml-base/>
      </wrapper>
    </p:with-input>
    <p:with-input port="insertion" pipe="@example"/>
  </p:insert>

  <p:insert position="first-child" match="/wrapper/with-add-xml-base">
    <p:with-input port="insertion" pipe="@fixup"/>
  </p:insert>
</p:declare-step>
