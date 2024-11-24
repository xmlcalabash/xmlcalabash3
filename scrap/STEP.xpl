<p:declare-step version="3.0"
                type="test:step"
                xmlns:p="http://www.w3.org/ns/xproc"
                visibility="private"
                xmlns:test="http://test">
    <p:option name="opt" static="true" select="3+4"/>
    <p:declare-step type="test:step2">
      <p:output port="result"/>
      <p:identity><p:with-input><doc/></p:with-input></p:identity>
    </p:declare-step>
    <test:step2/>
</p:declare-step>
