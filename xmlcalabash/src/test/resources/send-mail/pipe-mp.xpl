<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">
<p:output port="result"/>

<p:option name="SHOST" select="'localhost'" static="true"/>
<p:option name="SMTPPORT" select="1025" static="true"/>
<p:option name="APIPORT" select="1080" static="true"/>

<p:send-mail parameters="map{'host': $SHOST, 'port': $SMTPPORT}"
             auth="map{'username':'username','password':'password'}">
  <p:with-input>
    <p:inline>
      <emx:Message
          xmlns:emx='URN:ietf:params:email-xml:'
          xmlns:rfc822='URN:ietf:params:rfc822:'>
        <rfc822:from>
          <emx:Address>
            <emx:adrs>mailto:user@example.com</emx:adrs>
            <emx:name>Example User</emx:name>
          </emx:Address>
        </rfc822:from>
        <rfc822:to>
          <emx:Address>
            <emx:adrs>mailto:anotheruser@example.com</emx:adrs>
            <emx:name>Another User</emx:name>
          </emx:Address>
        </rfc822:to>
        <rfc822:subject>Multi-part Email</rfc822:subject>
        <emx:content type='text/plain'>
          This is my text message.
        </emx:content>
      </emx:Message>
    </p:inline>
    <p:inline>
      <html xmlns="http://www.w3.org/1999/xhtml">
        <head>
          <title>Hello, World</title>
        </head>
        <body>
          <h1>Hello, World!</h1>
          <p>This is my attachment.</p>
        </body>
      </html>
    </p:inline>
  </p:with-input>
</p:send-mail>

</p:declare-step>
