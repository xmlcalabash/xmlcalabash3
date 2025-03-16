<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:ex="http://docs.xmlcalabash.com/ns/example"
                exclude-inline-prefixes="#all"
                name="main" version="3.0">
  <p:import href="add-status.xpl"/>
  <p:output port="result" serialization="map{'indent':true()}"/>

  <p:variable name="today" select="current-dateTime()"/>

  <p:insert match="/*">
    <p:with-input port="source">
      <doc/>
    </p:with-input>
    <p:with-input port="insertion">
      <meta>
        <date>{$today}</date>
      </meta>
    </p:with-input>
  </p:insert>

  <ex:add-status
      status="{if (format-dateTime($today, '[F]') = 'Friday')
               then 'final'
               else 'draft'}"/>

</p:declare-step>
