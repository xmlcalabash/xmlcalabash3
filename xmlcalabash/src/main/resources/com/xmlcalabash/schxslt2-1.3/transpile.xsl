<!--
Copyright (C) by David Maus <dmaus@dmaus.name>

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use, copy,
modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
-->
<transform version="3.0" expand-text="yes" exclude-result-prefixes="schxslt sch xs" xpath-default-namespace="http://www.w3.org/1999/XSL/Transform"
               xmlns:alias="http://www.w3.org/1999/XSL/TransformAlias"
               xmlns:schxslt="http://dmaus.name/ns/2023/schxslt"
               xmlns:sch="http://purl.oclc.org/dsdl/schematron"
               xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns="http://www.w3.org/1999/XSL/Transform">

  <namespace-alias result-prefix="#default" stylesheet-prefix="alias"/>

  <param name="schxslt:debug" static="yes" select="false()">
    <!--
        Enable or disable debugging. When debugging is enable, the validation stylesheet is indented. Defaults to false.
    -->
  </param>

  <output indent="yes" use-when="$schxslt:debug"/>

  <variable name="schxslt:version" as="xs:string"
                select="if (starts-with('1.3', '$')) then 'development' else '1.3'"/>

  <param name="schxslt:phase" as="xs:string" select="'#DEFAULT'">
    <!--
        Name of the validation phase. The value '#DEFAULT' selects the pattern in the sch:schema/@defaultPhase attribute
        or '#ALL' if this attribute is not present. The value '#ALL' selects all patterns. Defaults to '#DEFAULT'.
    -->
  </param>

  <param name="schxslt:expand-text" as="xs:boolean" select="false()">
    <!--
        When set to boolean true, the validation stylesheet globally enables text value templates and you may use them
        in assertion or diagnostic messages. Defaults to false.
    -->
  </param>

  <param name="schxslt:streamable" as="xs:boolean" select="false()" static="yes">
    <!--
        Set to boolean true to create a streamable validation stylesheet. This *does not* check the streamability of
        XPath expressions in rules, assertions, variables etc. It merely declares the modes in the validation stylesheet
        to be streamable and removes the @location attribute from the SVRL output when no location function is given
        because the default fn:path() is not streamable. Defaults to false.
    -->
  </param>

  <param name="schxslt:location-function" as="xs:string?" select="()" static="yes">
    <!--
        Name of a function f($context as node()) as xs:string that provides location information for the SVRL
        report. Defaults to fn:path() when not set.
    -->
  </param>

  <param name="schxslt:fail-early" as="xs:boolean" select="false()" static="yes">
    <!--
        When set to boolean true, the validation stylesheet stops as soon as it encounters the first failed assertion
        or successful report. Defaults to false.
    -->
  </param>

  <param name="schxslt:terminate-validation-on-error" as="xs:boolean" select="true()" static="yes">
    <!--
        When set to boolean true, the validation stylesheet terminates the XSLT processor when it encounters a dynamic
        error. Defaults to true.
    -->
  </param>

  <variable name="schxslt:avt-attributes" as="xs:QName*">
    <sequence select="QName('', 'role')"/>
    <sequence select="QName('','flag')"/>
  </variable>

  <mode name="schxslt:expand" on-no-match="shallow-copy"/>
  <mode name="schxslt:include" on-no-match="shallow-copy"/>
  <mode name="schxslt:transpile" on-no-match="shallow-skip"/>

  <mode on-no-match="shallow-skip"/>
  <mode name="schxslt:copy-verbatim" on-no-match="shallow-copy"/>
  <mode name="schxslt:copy-message-content" on-no-match="shallow-copy"/>

  <key name="schxslt:patternByPhaseId" match="sch:pattern" use="../sch:phase[sch:active/@pattern = current()/@id]/@id"/>
  <key name="schxslt:patternByPhaseId" match="sch:pattern" use="'#ALL'"/>
  <key name="schxslt:phaseByPatternId" match="sch:phase" use="sch:active/@pattern"/>
  <key name="schxslt:diagnosticById" match="sch:diagnostic" use="@id"/>
  <key name="schxslt:propertyById" match="sch:property" use="@id"/>

  <template match="sch:schema" as="element(Q{http://www.w3.org/1999/XSL/Transform}stylesheet)">

    <variable name="schema" as="document-node(element(sch:schema))">
      <document>
        <call-template name="schxslt:perform-expand">
          <with-param name="schema" as="document-node(element(sch:schema))">
            <document>
              <call-template name="schxslt:perform-include">
                <with-param name="schema" as="element(sch:schema)" select="."/>
              </call-template>
            </document>
          </with-param>
        </call-template>
      </document>
    </variable>

    <apply-templates select="$schema" mode="schxslt:transpile"/>

  </template>

  <template name="schxslt:perform-include" as="element(sch:schema)">
    <param name="schema" as="element(sch:schema)" required="yes"/>
    <apply-templates select="$schema" mode="schxslt:include"/>
  </template>

  <template name="schxslt:perform-expand" as="document-node(element(sch:schema))">
    <param name="schema" as="document-node(element(sch:schema))" required="yes"/>
    <apply-templates select="$schema" mode="schxslt:expand"/>
  </template>

  <!-- Step 1: Include -->
  <template match="sch:include" as="element()" mode="schxslt:include">
    <variable name="external" as="element()" select="if (document(@href) instance of document-node()) then document(@href)/*[1] else document(@href)"/>
    <apply-templates select="$external" mode="#current">
      <with-param name="sourceLanguage" as="xs:string" select="schxslt:in-scope-language(.)"/>
      <with-param name="targetNamespaces" as="element(sch:ns)*" select="$external/ancestor::sch:schema/sch:ns"/>
    </apply-templates>
  </template>

  <template match="sch:rule/sch:extends[@href]" as="node()*" mode="schxslt:include">
    <variable name="external" as="element()" select="if (document(@href) instance of document-node()) then document(@href)/*[1] else document(@href)"/>
    <if test="(namespace-uri($external) ne 'http://purl.oclc.org/dsdl/schematron') or (local-name($external) ne 'rule')">
      <variable name="message" as="xs:string+">
        The @href attribute of an &lt;extends&gt; element must be an IRI reference to an external well-formed XML
        document or to an element in an external well-formed XML document that is a Schematron &lt;rule&gt;
        element. This @href points to a Q{{{namespace-uri($external)}}}{local-name($external)} element.
      </variable>
      <message terminate="yes">
        <text/>
        <value-of select="normalize-space(string-join($message))"/>
      </message>
    </if>
    <apply-templates select="$external/node()" mode="#current">
      <with-param name="sourceLanguage" select="schxslt:in-scope-language(.)"/>
      <with-param name="targetNamespaces" as="element(sch:ns)*" select="$external/../../sch:ns"/>
    </apply-templates>
  </template>

  <template match="*" mode="schxslt:include schxslt:expand">
    <param name="sourceLanguage" as="xs:string" select="schxslt:in-scope-language(.)"/>
    <param name="targetNamespaces" as="element(sch:ns)*"/>
    <variable name="inScopeLanguage" as="xs:string" select="schxslt:in-scope-language(.)"/>

    <copy>
      <for-each select="$targetNamespaces">
        <namespace name="{@prefix}" select="@uri"/>
      </for-each>
      <apply-templates select="@*" mode="#current"/>
      <if test="not(@xml:lang) and not($inScopeLanguage eq $sourceLanguage)">
        <attribute name="xml:lang" select="$inScopeLanguage"/>
      </if>
      <apply-templates select="node()" mode="#current"/>
    </copy>
  </template>

  <!-- Step 2: Expand -->
  <template match="sch:rule[@abstract = 'true'] | sch:pattern[@abstract = 'true']" as="empty-sequence()" mode="schxslt:expand"/>

  <template match="sch:rule/sch:extends[@rule]" as="node()*" mode="schxslt:expand">
    <variable name="abstract-rule" as="element(sch:rule)*"
                  select="(../../sch:rule, ../../../sch:rules/sch:rule)[@abstract = 'true'][@id = current()/@rule]"/>
    <if test="empty($abstract-rule)">
      <variable name="message" as="xs:string+">
        The current pattern or schema defines no abstract rule named '{@rule}'.
      </variable>
      <message terminate="yes">
        <text/>
        <value-of select="normalize-space(string-join($message))"/>
      </message>
    </if>
    <apply-templates select="$abstract-rule/node()" mode="#current">
      <with-param name="sourceLanguage" as="xs:string" select="schxslt:in-scope-language(.)"/>
    </apply-templates>
  </template>

  <template match="sch:pattern[@is-a]" as="element(sch:pattern)" mode="schxslt:expand">
    <variable name="is-a" as="element(sch:pattern)?" select="../sch:pattern[@abstract = 'true'][@id = current()/@is-a]"/>
    <if test="empty($is-a)">
      <variable name="message" as="xs:string+">
        The current schema does not define an abstract pattern with an id of '{@is-a}'.
      </variable>
      <message terminate="yes">
        <text/>
        <value-of select="normalize-space(string-join($message))"/>
      </message>
    </if>

    <!-- Check if all declared parameters are supplied -->
    <variable name="params-supplied" as="element(sch:param)*" select="sch:param"/>
    <variable name="params-declared" as="element(sch:param)*" select="$is-a/sch:param"/>
    <if test="exists($params-declared[empty(@value)][not(@name = $params-supplied/@name)])">
      <variable name="message" as="xs:string+">
        Some abstract pattern parameters of '{@is-a}' are declared but not supplied: {$params-declared[not(@name = $params-supplied/@name)]/@name}.
      </variable>
      <message terminate="yes">
        <text/>
        <value-of select="normalize-space(string-join($message))"/>
      </message>
    </if>
    <!-- Check if all supplied parameters are declared -->
    <if test="exists($params-declared) and exists($params-supplied[not(@name = $params-declared/@name)])">
      <variable name="message" as="xs:string+">
        Some abstract pattern parameters of '{@is-a}' are supplied but not declared: {$params-supplied[not(@name = $params-declared/@name)]/@name}.
      </variable>
      <message terminate="yes">
        <text/>
        <value-of select="normalize-space(string-join($message))"/>
      </message>
    </if>

    <variable name="instance" as="document-node()">
      <!-- In order to make use of fn:key() in the transpilation stage
           we need to root the preprocessed schema. -->
      <document>
        <apply-templates select="$is-a/node()" mode="#current">
          <with-param name="sourceLanguage" as="xs:string" select="schxslt:in-scope-language(.)"/>
          <with-param name="params" as="element(sch:param)*" select="($params-supplied, $params-declared[not(@name = $params-supplied/@name)][@value])" tunnel="yes"/>
        </apply-templates>
      </document>
    </variable>

    <variable name="diagnostics" as="xs:string*" select="tokenize(string-join($instance/sch:rule/sch:*/@diagnostics, ' '))"/>
    <variable name="properties" as="xs:string*" select="tokenize(string-join($instance/sch:rule/sch:*/@properties, ' '))"/>

    <copy>
      <apply-templates select="@*" mode="#current">
        <with-param name="params" as="element(sch:param)*" select="sch:param" tunnel="yes"/>
      </apply-templates>
      <if test="empty(@documents)">
        <apply-templates select="$is-a/@documents" mode="#current">
          <with-param name="params" as="element(sch:param)*" select="sch:param" tunnel="yes"/>
        </apply-templates>
      </if>
      <if test="empty(@xml:lang) and (schxslt:in-scope-language(.) ne schxslt:in-scope-language($is-a))">
        <attribute name="xml:lang" select="schxslt:in-scope-language($is-a)"/>
      </if>
      <sequence select="$instance"/>
      <apply-templates select="node()" mode="#current"/>

      <if test="exists($diagnostics)">
        <element name="diagnostics" namespace="http://purl.oclc.org/dsdl/schematron">
          <apply-templates select="key('schxslt:diagnosticById', $diagnostics)" mode="#current">
            <with-param name="params" as="element(sch:param)*" select="sch:param" tunnel="yes"/>
          </apply-templates>
        </element>
      </if>
      <if test="exists($properties)">
        <element name="properties" namespace="http://purl.oclc.org/dsdl/schematron">
          <apply-templates select="key('schxslt:propertyById', $properties)" mode="#current">
            <with-param name="params" as="element(sch:param)*" select="sch:param" tunnel="yes"/>
          </apply-templates>
        </element>
      </if>

    </copy>

  </template>

  <template match="sch:assert/@test | sch:report/@test | sch:rule/@context | sch:value-of/@select | sch:pattern/@documents | sch:name/@path | sch:let/@value | Q{http://www.w3.org/1999/XSL/Transform}copy-of[ancestor::sch:property]/@select" mode="schxslt:expand">
    <param name="params" as="element(sch:param)*" tunnel="yes"/>
    <attribute name="{name()}" select="schxslt:replace-params(., $params)"/>
  </template>

  <function name="schxslt:replace-params" as="xs:string?">
    <param name="src" as="xs:string"/>
    <param name="params" as="element(sch:param)*"/>
    <choose>
      <when test="empty($params)">
        <value-of select="$src"/>
      </when>
      <otherwise>
        <variable name="paramsSorted" as="element(sch:param)*">
          <for-each select="$params">
            <sort select="string-length(@name)" order="descending"/>
            <sequence select="."/>
          </for-each>
        </variable>

        <variable name="value" select="replace(replace($paramsSorted[1]/@value, '\\', '\\\\'), '\$', '\\\$')"/>
        <variable name="src" select="replace($src, concat('(\W*)\$', $paramsSorted[1]/@name, '(\W*)'), concat('$1', $value, '$2'))"/>
        <value-of select="schxslt:replace-params($src, $paramsSorted[position() > 1])"/>
      </otherwise>
    </choose>
  </function>

  <!-- Step 3: Transpile -->
  <template match="sch:schema" as="element(Q{http://www.w3.org/1999/XSL/Transform}stylesheet)" mode="schxslt:transpile">

    <variable name="phase" as="xs:string" select="if ($schxslt:phase = ('#DEFAULT', '')) then (@defaultPhase, '#ALL')[1] else $schxslt:phase"/>
    <variable name="patterns" as="map(xs:string, element(sch:pattern)+)">
      <map>
        <for-each-group select="key('schxslt:patternByPhaseId', $phase)" group-by="string(@documents)">
          <map-entry key="concat('group.', generate-id(current-group()[1]))" select="current-group()"/>
        </for-each-group>
      </map>
    </variable>

    <alias:stylesheet version="3.0" expand-text="{$schxslt:expand-text}">
      <for-each select="sch:ns">
        <namespace name="{@prefix}" select="@uri"/>
      </for-each>

      <alias:variable name="Q{{http://dmaus.name/ns/2023/schxslt}}phase" as="Q{{http://www.w3.org/2001/XMLSchema}}string" select="'{$phase}'"/>

      <apply-templates select="sch:let" mode="#current"/>
      <apply-templates select="sch:phase[@id = $phase]/sch:let" mode="#current"/>

      <sequence select="Q{http://www.w3.org/1999/XSL/Transform}accumulator | Q{http://www.w3.org/1999/XSL/Transform}function | Q{http://www.w3.org/1999/XSL/Transform}include | Q{http://www.w3.org/1999/XSL/Transform}import | Q{http://www.w3.org/1999/XSL/Transform}import-schema | Q{http://www.w3.org/1999/XSL/Transform}key | Q{http://www.w3.org/1999/XSL/Transform}use-package"/>

      <for-each select="Q{http://www.w3.org/2005/xpath-functions/map}keys($patterns)">
        <alias:mode name="{.}" on-no-match="shallow-skip" streamable="{$schxslt:streamable}"/>
        <alias:template match="*" mode="{.}" priority="-10">
          <alias:apply-templates select="@*" mode="#current"/>
          <alias:apply-templates select="node()" mode="#current"/>
        </alias:template>
        <apply-templates select="Q{http://www.w3.org/2005/xpath-functions/map}get($patterns, .)/sch:let" mode="#current"/>
        <apply-templates select="Q{http://www.w3.org/2005/xpath-functions/map}get($patterns, .)/sch:rule" mode="#current">
          <with-param name="mode" as="xs:string" select="."/>
        </apply-templates>
      </for-each>

      <alias:template match="root()" as="element(svrl:schematron-output)">

        <svrl:schematron-output>
          <call-template name="schxslt:copy-attributes">
            <with-param name="attributes" as="attribute()*" select="(@schemaVersion)"/>
          </call-template>
          <attribute name="phase" select="$phase"/>
          <for-each select="sch:ns">
            <svrl:ns-prefix-in-attribute-values prefix="{@prefix}" uri="{@uri}"/>
          </for-each>

          <comment>SchXslt2 Core {$schxslt:version}</comment>

          <alias:try>
            <for-each select="Q{http://www.w3.org/2005/xpath-functions/map}keys($patterns)">
              <variable name="groupId" as="xs:string" select="."/>
              <for-each select="Q{http://www.w3.org/2005/xpath-functions/map}get($patterns, $groupId)">
                <svrl:active-pattern>
                  <call-template name="schxslt:copy-attributes">
                    <with-param name="attributes" as="attribute()*" select="(@id)"/>
                  </call-template>
                  <alias:attribute name="documents" select="{if (@documents) then @documents else 'document-uri(.)'}"/>
                </svrl:active-pattern>
              </for-each>

              <choose>
                <when test="Q{http://www.w3.org/2005/xpath-functions/map}get($patterns, $groupId)[1]/@documents">
                  <alias:for-each select="{Q{http://www.w3.org/2005/xpath-functions/map}get($patterns, $groupId)[1]/@documents}">
                    <alias:source-document href="{{.}}">
                      <alias:apply-templates select="." mode="{$groupId}"/>
                    </alias:source-document>
                  </alias:for-each>
                </when>
                <otherwise>
                  <alias:apply-templates select="." mode="{$groupId}"/>
                </otherwise>
              </choose>

            </for-each>
            <if test="$schxslt:fail-early">
              <alias:catch errors="Q{{http://dmaus.name/ns/2023/schxslt}}CatchFailEarly">
                <alias:sequence select="$Q{{http://www.w3.org/2005/xqt-errors}}value"/>
              </alias:catch>
            </if>
            <alias:catch>
              <svrl:error code="{{$Q{{http://www.w3.org/2005/xqt-errors}}code}}">
                <alias:if test="document-uri()">
                  <alias:attribute name="document" select="document-uri()"/>
                </alias:if>
                <alias:if test="$Q{{http://www.w3.org/2005/xqt-errors}}description">
                  <alias:value-of select="$Q{{http://www.w3.org/2005/xqt-errors}}description"/>
                </alias:if>
              </svrl:error>
              <if test="$schxslt:terminate-validation-on-error">
                <alias:variable name="message" as="Q{{http://www.w3.org/2001/XMLSchema}}string+" expand-text="yes">
                  Running the ISO Schematron validation failed with a dynamic error.
                  Error code: {{$Q{{http://www.w3.org/2005/xqt-errors}}code}} Reason: {{$Q{{http://www.w3.org/2005/xqt-errors}}description}}
                </alias:variable>
                <alias:message terminate="yes" error-code="Q{{{{http://dmaus.name/ns/2023/schxslt}}}}ValidationError">
                  <alias:text/>
                  <alias:value-of select="normalize-space(string-join($message))"/>
                </alias:message>
              </if>
            </alias:catch>
          </alias:try>
        </svrl:schematron-output>

      </alias:template>

    </alias:stylesheet>

  </template>

  <template match="sch:rule" as="element(Q{http://www.w3.org/1999/XSL/Transform}template)" mode="schxslt:transpile">
    <param name="mode" as="xs:string" required="yes"/>

    <alias:template match="{@context}" mode="{$mode}" priority="{last() - position()}">
      <alias:param name="Q{{http://dmaus.name/ns/2023/schxslt}}pattern" as="Q{{http://www.w3.org/2001/XMLSchema}}string*" select="()"/>
      <alias:choose>
        <alias:when test="'{generate-id(..)}' = $Q{{http://dmaus.name/ns/2023/schxslt}}pattern">
          <svrl:suppressed-rule>
            <call-template name="schxslt:copy-attributes">
              <with-param name="attributes" as="attribute()*" select="(@id, @role, @flag, @context)"/>
            </call-template>
            <alias:if test="document-uri()">
              <alias:attribute name="document" select="document-uri()"/>
            </alias:if>
          </svrl:suppressed-rule>
          <alias:next-match>
            <alias:with-param name="Q{{http://dmaus.name/ns/2023/schxslt}}pattern" as="Q{{http://www.w3.org/2001/XMLSchema}}string*" select="$Q{{http://dmaus.name/ns/2023/schxslt}}pattern"/>
          </alias:next-match>
        </alias:when>
        <alias:otherwise>
          <svrl:fired-rule>
            <call-template name="schxslt:copy-attributes">
              <with-param name="attributes" as="attribute()*" select="(@id, @role, @flag, @context)"/>
            </call-template>
            <alias:if test="document-uri()">
              <alias:attribute name="document" select="document-uri()"/>
            </alias:if>
          </svrl:fired-rule>
          <apply-templates select="sch:let" mode="#current"/>
          <apply-templates select="sch:assert | sch:report" mode="#current"/>
          <alias:next-match>
            <alias:with-param name="Q{{http://dmaus.name/ns/2023/schxslt}}pattern" as="Q{{http://www.w3.org/2001/XMLSchema}}string*" select="('{generate-id(..)}', $Q{{http://dmaus.name/ns/2023/schxslt}}pattern)"/>
          </alias:next-match>
        </alias:otherwise>
      </alias:choose>
    </alias:template>

  </template>

  <template match="sch:schema/sch:let" as="element(Q{http://www.w3.org/1999/XSL/Transform}param)" mode="schxslt:transpile">
    <alias:param name="{@name}">
      <call-template name="schxslt:copy-attributes">
        <with-param name="attributes" as="attribute()*" select="(@as)"/>
      </call-template>
      <choose>
        <when test="@value">
          <attribute name="select" select="schxslt:protect-curlies(@value)"/>
        </when>
        <otherwise>
          <if test="not(@as)">
            <attribute name="as">node()*</attribute>
          </if>
          <apply-templates select="node()" mode="schxslt:copy-verbatim"/>
        </otherwise>
      </choose>
    </alias:param>
  </template>

  <template match="sch:let" as="element(Q{http://www.w3.org/1999/XSL/Transform}variable)" mode="schxslt:transpile">
    <alias:variable name="{@name}">
      <call-template name="schxslt:copy-attributes">
        <with-param name="attributes" as="attribute()*" select="(@as)"/>
      </call-template>
      <choose>
        <when test="@value">
          <attribute name="select" select="schxslt:protect-curlies(@value)"/>
        </when>
        <otherwise>
          <if test="not(@as)">
            <attribute name="as">node()*</attribute>
          </if>
          <apply-templates select="node()" mode="schxslt:copy-verbatim"/>
        </otherwise>
      </choose>
    </alias:variable>
  </template>

  <template match="sch:assert" as="element(Q{http://www.w3.org/1999/XSL/Transform}if)" mode="schxslt:transpile">
    <alias:if test="not({@test})">
      <alias:variable name="failed-assert" as="element(svrl:failed-assert)">
        <svrl:failed-assert>
          <call-template name="schxslt:failed-assertion-content"/>
        </svrl:failed-assert>
      </alias:variable>
      <if test="$schxslt:fail-early">
        <alias:message  select="$failed-assert" error-code="Q{{http://dmaus.name/ns/2023/schxslt}}CatchFailEarly" terminate="yes"/>
      </if>
      <alias:sequence select="$failed-assert"/>
    </alias:if>
  </template>

  <template match="sch:report" as="element(Q{http://www.w3.org/1999/XSL/Transform}if)" mode="schxslt:transpile">
    <alias:if test="{@test}">
      <alias:variable name="successful-report" as="element(svrl:successful-report)">
        <svrl:successful-report>
          <call-template name="schxslt:failed-assertion-content"/>
        </svrl:successful-report>
      </alias:variable>
      <if test="$schxslt:fail-early">
        <alias:message  select="$successful-report" error-code="Q{{http://dmaus.name/ns/2023/schxslt}}CatchFailEarly" terminate="yes"/>
      </if>
      <alias:sequence select="$successful-report"/>
    </alias:if>
  </template>

  <template match="*" as="element()" mode="schxslt:copy-verbatim schxslt:copy-message-content">
    <alias:element name="{local-name()}" namespace="{namespace-uri()}">
      <apply-templates select="@*" mode="#current"/>
      <apply-templates select="node()" mode="#current"/>
    </alias:element>
  </template>

  <template match="@*" as="element()" mode="schxslt:copy-verbatim schxslt:copy-message-content">
    <alias:attribute name="{local-name()}" namespace="{namespace-uri()}">{.}</alias:attribute>
  </template>

  <template match="Q{http://www.w3.org/1999/XSL/Transform}copy-of[ancestor::sch:property]" as="element(Q{http://www.w3.org/1999/XSL/Transform}copy-of)" mode="schxslt:copy-message-content">
    <copy>
      <sequence select="@*"/>
      <sequence select="node()"/>
    </copy>
  </template>

  <template match="sch:name[@path]" as="element(Q{http://www.w3.org/1999/XSL/Transform}value-of)" mode="schxslt:copy-message-content">
    <alias:value-of select="{@path}"/>
  </template>

  <template match="sch:name[not(@path)]" as="element(Q{http://www.w3.org/1999/XSL/Transform}value-of)" mode="schxslt:copy-message-content">
    <alias:value-of select="name()"/>
  </template>

  <template match="sch:value-of" as="element(Q{http://www.w3.org/1999/XSL/Transform}value-of)" mode="schxslt:copy-message-content">
    <alias:value-of select="{@select}"/>
  </template>

  <template name="schxslt:report-message" as="element(svrl:text)?">
    <if test="text() | *">
      <svrl:text>
        <sequence select="@xml:*"/>
        <apply-templates select="node()" mode="schxslt:copy-message-content"/>
      </svrl:text>
    </if>
  </template>

  <template name="schxslt:report-diagnostics" as="element(svrl:diagnostic-reference)*">
    <variable name="diagnostics" as="xs:string*" select="tokenize(normalize-space(@diagnostics))"/>
    <for-each select="if (../../sch:diagnostics) then key('schxslt:diagnosticById', $diagnostics, ../..) else key('schxslt:diagnosticById', $diagnostics, ancestor::sch:schema)">
      <svrl:diagnostic-reference diagnostic="{schxslt:protect-curlies(@id)}">
        <svrl:text>
          <if test="schxslt:in-scope-language(.) ne schxslt:in-scope-language(ancestor::sch:schema)">
            <attribute name="xml:lang" select="schxslt:in-scope-language(.)"/>
          </if>
          <sequence select="@xml:space"/>
          <call-template name="schxslt:copy-attributes">
            <with-param name="attributes" as="attribute()*" select="(@see, @icon, @fpi)"/>
          </call-template>
          <apply-templates select="node()" mode="schxslt:copy-message-content"/>
        </svrl:text>
      </svrl:diagnostic-reference>
    </for-each>
  </template>

  <template name="schxslt:report-properties" as="element(svrl:property-reference)*">
    <variable name="properties" as="xs:string*" select="tokenize(normalize-space(@properties))"/>
    <for-each select="if (../../sch:properties) then key('schxslt:propertyById', $properties, ../..) else key('schxslt:propertyById', $properties, ancestor::sch:schema)">
      <svrl:property-reference property="{schxslt:protect-curlies(@id)}">
        <call-template name="schxslt:copy-attributes">
          <with-param name="attributes" as="attribute()*" select="(@role, @scheme)"/>
        </call-template>
        <svrl:text>
          <if test="schxslt:in-scope-language(.) ne schxslt:in-scope-language(ancestor::sch:schema)">
            <attribute name="xml:lang" select="schxslt:in-scope-language(.)"/>
          </if>
          <sequence select="@xml:space"/>
          <call-template name="schxslt:copy-attributes">
            <with-param name="attributes" as="attribute()*" select="(@see, @icon, @fpi)"/>
          </call-template>
          <apply-templates select="node()" mode="schxslt:copy-message-content"/>
        </svrl:text>
      </svrl:property-reference>
    </for-each>
  </template>

  <template name="schxslt:failed-assertion-content" as="node()+">
    <call-template name="schxslt:copy-attributes">
      <with-param name="attributes" as="attribute()*" select="(@flag, @id, @role, @test)"/>
    </call-template>
    <if test="schxslt:in-scope-language(.) ne schxslt:in-scope-language(ancestor::sch:schema)">
      <attribute name="xml:lang" select="schxslt:in-scope-language(.)"/>
    </if>
    <if test="not($schxslt:streamable) or exists($schxslt:location-function)">
      <alias:attribute name="location" select="{($schxslt:location-function, 'path')[1]}(.)"/>
    </if>
    <call-template name="schxslt:report-diagnostics"/>
    <call-template name="schxslt:report-properties"/>
    <call-template name="schxslt:report-message"/>
  </template>

  <template name="schxslt:copy-attributes" as="attribute()*">
    <param name="attributes" as="attribute()*" required="yes"/>
    <for-each select="$attributes">
      <attribute name="{name()}" select="if (node-name() = $schxslt:avt-attributes) then . else schxslt:protect-curlies(.)"/>
    </for-each>
  </template>

  <function name="schxslt:in-scope-language" as="xs:string?">
    <param name="context" as="node()"/>
    <value-of select="lower-case($context/ancestor-or-self::*[@xml:lang][1]/@xml:lang)"/>
  </function>

  <function name="schxslt:protect-curlies" as="xs:string">
    <param name="value" as="xs:string"/>
    <value-of select="$value => replace('\{', '{{') => replace('\}', '}}')"/>
  </function>

</transform>
