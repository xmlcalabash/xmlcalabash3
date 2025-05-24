<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">
<p:output port="result"/>

<p:option name="SHOST" select="'localhost'" static="true"/>
<p:option name="SMTPPORT" select="1025" static="true"/>
<p:option name="APIPORT" select="1080" static="true"/>

<p:send-mail parameters="map{'host': $SHOST, 'port': $SMTPPORT, 'debug':false()}"
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
        <rfc822:subject>Plain text email</rfc822:subject>
        <emx:content type='text/plain'>
          Do that thing.
        </emx:content>
      </emx:Message>
    </p:inline>
  </p:with-input>
</p:send-mail>

</p:declare-step>
