<s:schema queryBinding="xslt2"
          xmlns:s="http://purl.oclc.org/dsdl/schematron">
  <s:ns prefix="sr" uri="http://www.w3.org/2005/sparql-results#"/>
  <s:pattern>
    <s:rule context="/">
      <s:assert test="sr:sparql">The root is wrong.</s:assert>
    </s:rule>
    <s:rule context="/sr:sparql">
      <s:assert test="sr:head">There’s no head</s:assert>
      <s:assert test="sr:results">There are no results</s:assert>
    </s:rule>
    <s:rule context="/sr:sparql/sr:head">
      <s:assert test="count(sr:variable) = 2">Wrong number of headers</s:assert>
    </s:rule>
    <s:rule context="/sr:sparql/sr:results">
      <s:assert test="count(sr:result) = 3">Wrong number of results</s:assert>
    </s:rule>
  </s:pattern>
</s:schema>
