<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:mp="http://docbook.org/ns/docbook/modes/private"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:rddl="http://www.rddl.org/"
                xmlns:t="http://docbook.org/ns/docbook/templates"
                xmlns:tp="http://docbook.org/ns/docbook/templates/private"
                xmlns:v="http://docbook.org/ns/docbook/variables"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                default-mode="m:docbook"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:template match="db:book" mode="m:generate-titlepage">
  <header>
    <div class="cover">
      <div class="image">
        <img src="img/xmlcalabash.png" width="400"/>
      </div>
      <div class="text">
        <div class="fill">
          <h1><xsl:value-of select="db:info/db:title"/></h1>
<!--
          <div class="authorgroup">
            <span class="label">
              <xsl:text>Author: </xsl:text>
            </span>
            <xsl:apply-templates select="db:info/db:author"/>
          </div>
-->
          <div class="docversion">
            <xsl:text>Version </xsl:text>
            <xsl:sequence select="$BUILDINFO/ref-version/string()"/>
          </div>
          <div class="version">
            <xsl:text>for XML Calabash </xsl:text>
            <span title="Build id: {$BUILDINFO/build-id}">{$BUILDINFO/version}</span>
          </div>
          <div class="date">
            <xsl:text>Updated: </xsl:text>
            <xsl:apply-templates select="format-date(current-date(), '[D01] [MNn,*-3] [Y0001]')"/>
          </div>
          <!--
          <xsl:if test="$output-media = 'print'">
            <xsl:apply-templates select="db:info/db:revhistory"/>
          </xsl:if>
          -->
          <p class="copyright">
            <a href="copyright{$html-extension}">Copyright</a>
            <xsl:text> &#xA9; </xsl:text>
            <xsl:value-of select="/db:book/db:info/db:copyright/db:year[1]"/>
            <xsl:if test="/db:book/db:info/db:copyright/db:year[2]">
              <xsl:text>–</xsl:text>
              <xsl:value-of select="/db:book/db:info/db:copyright/db:year[last()]"/>
            </xsl:if>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/db:book/db:info/db:copyright/db:holder"/>
            <xsl:text>.</xsl:text>
          </p>
        </div>
      </div>
      <div class="docslink"><a href="https://docs.xmlcalabash.com">docs.xmlcalabash.com</a></div>
    </div>
  </header>
</xsl:template>

<xsl:template match="db:impl">
  <span class="impl">
    <xsl:apply-templates/>
  </span>
</xsl:template>

<!-- ============================================================ -->
<!-- The refsynopsisdiv -->

<xsl:template match="db:refsynopsisdiv/db:refsection[contains-token(@role, 'introduction')]">
  <xsl:apply-templates select="node() except db:info"/>
</xsl:template>

<xsl:template match="db:refsynopsisdiv/db:refsection[contains-token(@role, 'errors')]">
</xsl:template>

<xsl:template match="db:refsynopsisdiv/db:refsection[contains-token(@role, 'implementation-features')]">
</xsl:template>

<xsl:template match="db:refsynopsisdiv/db:refsection[contains-token(@role, 'step-declaration')]">
  <xsl:apply-templates select="node() except db:info"/>

  <xsl:variable name="errors" select="../db:refsection[contains-token(@role, 'errors')]"/>
  <xsl:variable name="impls" select="../db:refsection[contains-token(@role, 'implementation-features')]"/>

  <xsl:variable name="errlist" as="element(db:error)*">
    <xsl:choose>
      <xsl:when test="$errors//db:error">
        <xsl:sequence select="$errors//db:error"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="ancestor::db:refentry//db:error"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="exists($errors) and empty($errlist)">
    <xsl:message>
      <xsl:text>Pointless errors in </xsl:text>
      <xsl:value-of select="ancestor::db:refentry/db:refnamediv/db:refname"/>
    </xsl:message>
  </xsl:if>

  <xsl:if test="exists($errlist)">
    <details>
      <summary>Errors</summary>
      <table class="tableaux error-list">
        <thead>
          <tr><th class="code">Code</th><th>Description</th></tr>
        </thead>
        <tbody>
          <xsl:for-each-group group-by="@code" select="$errlist">
            <xsl:sort select="@code"/>

            <xsl:variable name="code" as="xs:string" select="current-group()[1]/@code"/>

            <tr>
              <td class="code">
                <xsl:choose>
                  <xsl:when test="contains($code, ':')">
                    <code>{$code}</code>
                  </xsl:when>
                  <xsl:otherwise>
                    <a class="exlink" href="{$steps-base-uri}#err.{$code}">
                      <code>err:X{$code}</code>
                    </a>
                  </xsl:otherwise>
                </xsl:choose>
              </td>
              <td><xsl:apply-templates select="current-group()[1]/node()"/></td>
            </tr>
          </xsl:for-each-group>
        </tbody>
      </table>
    </details>
  </xsl:if>

  <xsl:if test="$impls//db:impl[db:glossterm = 'implementation-defined']">
    <details>
      <summary>Implementation defined features</summary>
      <ul>
        <xsl:for-each select="$impls//db:impl[db:glossterm = 'implementation-defined']">
          <li>
            <xsl:apply-templates select="node()"/>
          </li>
        </xsl:for-each>
      </ul>
    </details>
  </xsl:if>

  <xsl:if test="$impls//db:impl[db:glossterm = 'implementation-dependent']">
    <details>
      <summary>Implementation dependent features</summary>
      <ul>
        <xsl:for-each select="$impls//db:impl[db:glossterm = 'implementation-dependent']">
          <li>
            <xsl:apply-templates select="node()"/>
          </li>
        </xsl:for-each>
      </ul>
    </details>
  </xsl:if>
</xsl:template>

<!-- ============================================================ -->

<xsl:variable name="standard-map"
              select="map { '3.0': 'https://spec.xproc.org/3.0/steps/',
                            '3.1': 'https://spec.xproc.org/master/head/steps/',

'3.1/p:css-formatter': 'https://spec.xproc.org/master/head/paged-media#c.css-formatter',
'3.1/p:directory-list': 'https://spec.xproc.org/master/head/file#c.directory-list',
'3.1/p:file-copy': 'https://spec.xproc.org/master/head/file#c.file-copy',
'3.1/p:file-create-tempfile': 'https://spec.xproc.org/master/head/file#c.file-create-tempfile',
'3.1/p:file-delete': 'https://spec.xproc.org/master/head/file#c.file-delete',
'3.1/p:file-info': 'https://spec.xproc.org/master/head/file#c.file-info',
'3.1/p:file-mkdir': 'https://spec.xproc.org/master/head/file#c.file-mkdir',
'3.1/p:file-move': 'https://spec.xproc.org/master/head/file#c.file-move',
'3.1/p:file-touch': 'https://spec.xproc.org/master/head/file#c.file-touch',
'3.1/p:invisible-xml': 'https://spec.xproc.org/master/head/ixml#c.invisible-xml',
'3.1/p:markdown-to-html': 'https://spec.xproc.org/master/head/text#c.markdown-to-html',
'3.1/p:os-exec': 'https://spec.xproc.org/master/head/os#c.os-exec',
'3.1/p:os-info': 'https://spec.xproc.org/master/head/os#c.os-info',
'3.1/p:run': 'https://spec.xproc.org/master/head/run#c.run',
'3.1/p:send-mail': 'https://spec.xproc.org/master/head/mail#c.send-mail',
'3.1/p:validate-with-dtd': 'https://spec.xproc.org/master/head/validation#c.validate-with-dtd',
'3.1/p:validate-with-json-schema': 'https://spec.xproc.org/master/head/validation#c.validate-with-json-schema',
'3.1/p:validate-with-nvdl': 'https://spec.xproc.org/master/head/validation#c.validate-with-nvdl',
'3.1/p:validate-with-relax-ng': 'https://spec.xproc.org/master/head/validation#c.validate-with-relax-ng',
'3.1/p:validate-with-schematron': 'https://spec.xproc.org/master/head/validation#c.validate-with-schematron',
'3.1/p:validate-with-xml-schema': 'https://spec.xproc.org/master/head/validation#c.validate-with-xml-schema',
'3.1/p:xsl-formatter': 'https://spec.xproc.org/master/head/paged-media#c.xsl-formatter'

 }"/>

<xsl:variable name="not-xprocref" select="()"/>

<xsl:template match="db:para[@role='external-refs']">
  <xsl:if test="node()">
    <xsl:message terminate="yes">The ‘external-refs’ paragraph must be empty.</xsl:message>
  </xsl:if>

  <xsl:variable name="info" select="ancestor::db:refentry[1]/db:info"/>
  <xsl:variable name="name" select="ancestor::db:refentry[1]/db:refnamediv/db:refname"/>
  <xsl:variable name="version" select="($info/db:bibliomisc[@role='version']/string(), '3.0')[1]"/>

  <xsl:if test="not($version = ('3.0', '3.1'))">
    <xsl:message terminate="yes">Unsupported version {$version}.</xsl:message>
  </xsl:if>

  <xsl:variable name="link" as="xs:string">
    <xsl:choose>
      <xsl:when test="map:contains($standard-map, $version||'/'||$name)">
        <xsl:sequence select="map:get($standard-map, $version||'/'||$name)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="map:get($standard-map, $version) || '#' || replace($name, 'p:', 'c.')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <p>The <code>{$name}</code> step is a
  <a href="{$link}">standard XProc {$version} step</a>.
  <xsl:choose>
    <xsl:when test="$name = $not-xprocref">
      <xsl:message>Not on XProcRef.org? {$name}</xsl:message>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>It is also described on </xsl:text>
      <a href="https://xprocref.org/{$version}/{replace($name, ':', '.')}.html">XProcRef.org</a>
      <xsl:text>.</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  </p>
</xsl:template>

<xsl:template match="db:refentry" mode="m:toc-entry">
  <xsl:variable name="refmeta" select=".//db:refmeta"/>
  <xsl:variable name="refentrytitle" select="$refmeta//db:refentrytitle"/>
  <xsl:variable name="refnamediv" select=".//db:refnamediv"/>
  <xsl:variable name="refname" select="$refnamediv//db:refname"/>

  <xsl:variable name="title">
    <xsl:choose>
      <xsl:when test="$refentrytitle">
        <xsl:apply-templates select="$refentrytitle[1]">
          <xsl:with-param name="purpose" select="'lot'"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$refnamediv/db:refdescriptor">
        <xsl:apply-templates select="($refnamediv/db:refdescriptor)[1]">
          <xsl:with-param name="purpose" select="'lot'"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$refname">
        <xsl:apply-templates select="$refname[1]">
          <xsl:with-param name="purpose" select="'lot'"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="desc" select="db:refsection[db:info/db:title = 'Description']"/>
  <xsl:variable name="version" select="db:info/db:bibliomisc[@role = 'version']/string()"/>
  <xsl:variable name="augmented"
                select="count($desc/* except $desc/db:info) gt 1"/>

  <li>
    <span class="refentrytitle {if ($augmented) then 'impldetail' else 'standard'}">
      <a href="#{f:id(.)}">
        <xsl:sequence select="$title"/>
      </a>
    </span>
    <xsl:if test="f:is-true($annotate-toc)">
      <xsl:apply-templates select="(db:refnamediv/db:refpurpose)[1]">
        <xsl:with-param name="purpose" select="'lot'"/>
      </xsl:apply-templates>
    </xsl:if>
    <xsl:if test="$version">
      <span class="newinver">{$version}</span>
    </xsl:if>
  </li>
</xsl:template>

<xsl:template match="db:tag">
  <xsl:variable name="formatted" as="element()">
    <xsl:next-match/>
  </xsl:variable>

  <xsl:variable name="tagname" select="normalize-space(string(.))"/>

  <xsl:variable name="link-to" as="xs:string"
                select="replace($tagname, ':', '-')"/>

  <xsl:variable name="target" as="element()?"
                select="id($link-to)"/>

  <xsl:element name="{node-name($formatted)}"
               namespace="{namespace-uri-from-QName(node-name($formatted))}">
    <xsl:copy-of select="$formatted/@*"/>
    <xsl:choose>
      <xsl:when test="@class = 'attribute' or not(contains($tagname, ':')) or @namespace">
        <xsl:sequence select="$formatted/node()"/>
      </xsl:when>
      <xsl:when test="exists($target) and $target = ancestor::*">
        <xsl:sequence select="$formatted/node()"/>
      </xsl:when>

      <xsl:when test="$tagname = ('p:document', 'p:with-input')">
        <a href="https://spec.xproc.org/master/head/xproc/#p.{substring-after($tagname, 'p:')}">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>
      <xsl:when test="$tagname = 'xsl:message'">
        <a href="https://www.w3.org/TR/xslt-30/#element-message">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>

      <xsl:when test="starts-with($tagname, 'cc:')">
        <a href="https://docs.xmlcalabash.com/userguide/current/configuration.html#{replace($tagname,':','.')}">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>

      <xsl:when test="starts-with($tagname, 'xvrl:')">
        <a href="https://spec.xproc.org/master/head/xvrl/">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>

      <xsl:when test="starts-with($tagname, 'c:')">
        <xsl:sequence select="$formatted/node()"/>
      </xsl:when>

      <xsl:when test="exists($target)">
        <a href="#{$link-to}">
          <xsl:sequence select="$formatted/node()"/>
        </a>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message select="'No target:', $tagname"/>
        <xsl:sequence select="$formatted/node()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:element>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="db:varlistentry[contains-token(@role, 'diffs')]/db:listitem"
              mode="m:panelset">
  <p>This listing attempts to summarize what has changed from the original input.</p>

  <xsl:apply-templates mode="m:docbook"/>

  <p>When present, additions are shown in <code class="revadded">bold, green</code>;
  deletions are shown in
  <code class="revdeleted">italic, red with strike through</code>;
  and changed elements are shown in
  <code class="revchanged">bold, italic orange</code>.</p>
</xsl:template>

<!-- ============================================================ -->

</xsl:stylesheet>
