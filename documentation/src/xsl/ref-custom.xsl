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
            <xsl:sequence select="$VERSION?refVersion"/>
          </div>
          <div class="version">
            <xsl:text>for XML Calabash </xsl:text>
            <span title="{$VERSION?BUILD_HASH} {$VERSION?BUILD_DATE}">{$VERSION?VERSION}</span>
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

  <xsl:if test="$errors">
    <details>
      <summary>Errors</summary>
      <table class="tableaux error-list">
        <thead>
          <tr><th class="code">Code</th><th>Description</th></tr>
        </thead>
        <tbody>
          <xsl:for-each-group group-by="@code" select="$errors//db:error">
            <xsl:sort select="@code"/>

            <xsl:variable name="code" as="xs:string" select="current-group()[1]/@code"/>

            <tr>
              <td class="code">
                <a class="exlink" href="{$steps-base-uri}#err.{$code}">
                  <code>err:X{$code}</code>
                </a>
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
                            '3.1': 'https://spec.xproc.org/master/head/steps/' }"/>

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

  <p>The <code>{$name}</code> step is defined in the
  <a href="{map:get($standard-map, $version)}#{replace($name, 'p:', 'c.')}">XProc {$version}:
  Standard Step Library</a>. It is also described on
  <a href="https://xprocref.org/{$version}/{replace($name, ':', '.')}.html">XProcRef.org</a>.
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
