<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                exclude-inline-prefixes="cx xs"
                version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/pipeline-messages.xpl"/>

<p:output port="result"/>

<cx:pipeline-messages/>

</p:declare-step>
