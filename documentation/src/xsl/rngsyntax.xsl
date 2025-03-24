<?xml version='1.0' encoding='UTF-8'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:f="http://docbook.org/xslt/ns/extension"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:rng="http://relaxng.org/ns/structure/1.0"
                xmlns:sa="http://xproc.org/ns/syntax-annotations"
                xmlns:ss="http://xproc.org/ns/syntax-summary"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                default-mode="m:docbook"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:strip-space elements="rng:*"/>

<!-- ============================================================ -->

<xsl:template match="db:rng-pattern">
  <xsl:variable name="schema" select="doc(resolve-uri(@schema, base-uri(.)))"/>
  <xsl:variable name="name" select="string(@name)"/>
  <xsl:variable name="rngpat" select="$schema/rng:grammar/rng:define[@name = $name]"/>

  <xsl:if test="not($rngpat) or not($rngpat/rng:element)">
    <xsl:message>
      <xsl:text>Warning: Can't make syntax summary for </xsl:text>
      <xsl:value-of select="@name"/>
    </xsl:message>
  </xsl:if>

  <!--
  <xsl:message select="$rngpat"/>
  -->

  <xsl:apply-templates select="$rngpat">
    <xsl:with-param name="schema" tunnel="yes"
                    select="$schema/rng:grammar"/>
    <xsl:with-param name="prefix" tunnel="yes"
                    select="if (@prefix) then @prefix else substring-before($name, '.')"/>
    <xsl:with-param name="format" tunnel="yes"
                    select="@format"/>
    <xsl:with-param name="namespace" tunnel="yes"
                    select="@ns"/>
    <xsl:with-param name="xml-id" tunnel="yes" as="xs:string?">
      <xsl:choose>
        <xsl:when test="@xml:id">
          <xsl:sequence select="@xml:id"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="@name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:with-param>
    <xsl:with-param name="suppress-prefix" tunnel="yes"
                    select="@suppress-prefix"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="rng:define">
  <xsl:param name="schema" as="element(rng:grammar)" tunnel="yes"/>
  <xsl:param name="prefix" as="xs:string" tunnel="yes"/>
  <xsl:param name="format" as="xs:string?" tunnel="yes"/>
  <xsl:param name="namespace" as="xs:string?" tunnel="yes"/>
  <xsl:param name="xml-id" as="xs:string?" tunnel="yes"/>

  <xsl:variable name="rngpat" select="."/>
  <xsl:variable name="class" select="()"/>

  <xsl:variable name="summary-body" as="element()*">
    <xsl:apply-templates select="$rngpat/rng:element/*"/>
  </xsl:variable>

  <!--
  <xsl:message>
    <rng>
      <xsl:copy-of select="$summary-body"/>
    </rng>
  </xsl:message>
  -->

  <xsl:variable name="summary" as="element()*">
    <xsl:variable name="name" select="$rngpat/rng:element/@name/string()"/>

    <xsl:variable name="namespace" as="xs:string?">
      <xsl:if test="contains($name, ':')">
        <xsl:variable name="prefix" select="substring-before($name, ':')"/>
        <xsl:sequence select="$rngpat/rng:element/namespace::*[local-name(.) = $prefix]"/>
      </xsl:if>
    </xsl:variable>

    <ss:element-summary name="{$name}">
      <xsl:if test="$namespace">
        <xsl:attribute name="namespace" select="$namespace"/>
      </xsl:if>
      <xsl:if test="$xml-id != ''">
	<xsl:attribute name="xml:id" select="$xml-id"/>
      </xsl:if>

      <xsl:attribute name="prefix" select="$prefix"/>
      <xsl:attribute name="class" select="'language-construct'"/>

      <xsl:copy-of select="$summary-body[self::ss:attribute]"/>
      <xsl:if test="$summary-body[not(self::ss:attribute)]">
	<ss:content-model>
	  <xsl:copy-of select="$summary-body[not(self::ss:attribute)]"/>
	</ss:content-model>
      </xsl:if>
    </ss:element-summary>
  </xsl:variable>

  <!--
  <xsl:message>
    <xsl:sequence select="$summary"/>
  </xsl:message>
  -->

  <xsl:if test="not($class) or $rngpat/@sa:class = $class">
    <xsl:choose>
      <xsl:when test="$format = 'table'">
        <xsl:apply-templates select="$summary" mode="table"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="$summary"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<xsl:template match="rng:element">
  <xsl:param name="repeat" select="''"/>
  <xsl:choose>
    <xsl:when test="@name">
      <ss:element name="{@name}" repeat="{$repeat}"/>
    </xsl:when>
    <xsl:otherwise>
      <ss:element name="{{any-name}}" repeat="{$repeat}"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="rng:anyName"/>

<xsl:template match="rng:optional">
  <xsl:choose>
    <xsl:when test="count(*) &gt; 1">
      <ss:group type="sequence" repeat="?">
	<xsl:apply-templates/>
      </ss:group>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates>
	<xsl:with-param name="repeat" select="'?'"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="rng:ref">
  <xsl:param name="repeat" select="''"/>
  <xsl:param name="schema" tunnel="yes"/>

  <xsl:variable name="pattern" select="@name"/>
  <xsl:variable name="rngpat" select="$schema/rng:define[@name=$pattern]"/>

  <xsl:choose>
    <xsl:when test="$rngpat/@sa:ignore = 'yes'">
      <!-- nop -->
    </xsl:when>
    <xsl:when test="$rngpat/@sa:model">
      <ss:model name="{$rngpat/@sa:model}" repeat="{$repeat}"/>
    </xsl:when>
    <xsl:when test="$rngpat/@sa:element">
      <ss:element name="{$rngpat/@sa:element}" repeat="{$repeat}"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="$rngpat/*">
	<xsl:with-param name="repeat" select="$repeat"/>
        <xsl:with-param name="avt" select="@sa:avt"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="rng:choice">
  <xsl:param name="repeat" select="''"/>

  <xsl:variable name="content" as="element()*">
    <xsl:for-each select="*">
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:variable>

  <xsl:if test="$content">
    <ss:group type="choice" repeat="{$repeat}">
      <xsl:sequence select="$content"/>
    </ss:group>
  </xsl:if>
</xsl:template>

<xsl:template match="rng:group">
  <xsl:param name="repeat" select="''"/>

  <ss:group type="sequence" repeat="{$repeat}">
    <xsl:for-each select="*">
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </ss:group>
</xsl:template>

<xsl:template match="rng:zeroOrMore">
  <xsl:apply-templates>
    <xsl:with-param name="repeat" select="'*'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="rng:oneOrMore">
  <xsl:apply-templates>
    <xsl:with-param name="repeat" select="'+'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="rng:interleave">
  <xsl:param name="repeat" select="''"/>

  <xsl:variable name="content" as="element()*">
    <xsl:for-each select="*">
      <xsl:apply-templates select=".">
        <xsl:with-param name="repeat" select="$repeat"/>
      </xsl:apply-templates>
    </xsl:for-each>
  </xsl:variable>

  <xsl:if test="$content">
    <xsl:choose>
      <xsl:when test="//rng:attribute">
        <xsl:sequence select="$content"/>
      </xsl:when>
      <xsl:otherwise>
        <ss:group type="interleave" repeat="{$repeat}">
          <xsl:sequence select="$content"/>
        </ss:group>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<xsl:template match="rng:attribute[@name='version' and rng:value]" priority="20">
  <xsl:param name="schema" tunnel="yes"/>
  <xsl:param name="repeat" select="''"/>
  <xsl:param name="avt" select="()"/>

  <!-- A totally special case: allow the version attribute to be of type xs:decimal,
       but display its fixed value as "3.0" without the quotation marks that make
       it look like a string. -->

  <ss:attribute name="{@name}" optional="{$repeat}" avt="false" type="{string(rng:value)}"/>
</xsl:template>

<xsl:template match="rng:attribute[@name]" priority="10">
  <xsl:param name="schema" tunnel="yes"/>
  <xsl:param name="repeat" select="''"/>
  <xsl:param name="avt" select="()"/>

  <xsl:variable name="is-avt" select="($avt,@sa:avt)[1]"/>

  <ss:attribute name="{@name}" optional="{$repeat}" avt="{$is-avt}">
    <xsl:attribute name="type">
      <xsl:choose>
	<xsl:when test="rng:data">
	  <xsl:apply-templates select="rng:data"/>
	</xsl:when>
	<xsl:when test="rng:oneOrMore/rng:data">
	  <xsl:apply-templates/>
          <xsl:text>+</xsl:text>
	</xsl:when>
	<xsl:when test="rng:zeroOrMore/rng:data">
	  <xsl:apply-templates/>
          <xsl:text>*</xsl:text>
	</xsl:when>
	<xsl:when test="rng:ref">
	  <xsl:variable name="pattern" select="rng:ref/@name"/>
	  <xsl:variable name="rngpat" select="$schema/rng:define[@name=$pattern]"/>
	  <xsl:choose>
	    <xsl:when test="$rngpat/@sa:model">
	      <xsl:value-of select="$rngpat/@sa:model"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:message>
		<xsl:text>Warning: unsupported ref in attribute: </xsl:text>
		<xsl:value-of select="@name"/>
                <xsl:text>: </xsl:text>
                <xsl:value-of select="rng:ref/@name"/>
	      </xsl:message>
	    </xsl:otherwise>
	  </xsl:choose>
	</xsl:when>
	<xsl:when test="rng:choice/rng:value">
	  <xsl:for-each select="rng:choice/rng:value|rng:choice/rng:data">
	    <xsl:if test="position()&gt;1">|</xsl:if>
	    <xsl:choose>
	      <xsl:when test="self::rng:value">
		<xsl:value-of select="."/>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:text>xs:</xsl:text>
		<xsl:value-of select="@type"/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </xsl:for-each>
	</xsl:when>
	<xsl:when test="rng:value">
	  <xsl:text>"</xsl:text>
	  <xsl:value-of select="rng:value"/>
	  <xsl:text>"</xsl:text>
	</xsl:when>
	<xsl:when test="rng:text or not(* except db:purpose)">
	  <xsl:text>string</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:message>
	    <xsl:text>Warning: unsupported content in attribute: </xsl:text>
	    <xsl:value-of select="@name"/>
	    <xsl:text> (</xsl:text>
	    <xsl:value-of select="ancestor::rng:element/@name"/>
	    <xsl:text>)</xsl:text>
          </xsl:message>
          <xsl:message select="."/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <xsl:sequence select="db:purpose/node()"/>
  </ss:attribute>
</xsl:template>

<xsl:template match="rng:attribute">
  <xsl:param name="schema" tunnel="yes"/>
  <xsl:param name="repeat" select="''"/>

  <xsl:variable name="content">
    <xsl:sequence select="* except rng:anyName"/>
  </xsl:variable>

  <xsl:variable name="nsName" as="element()?">
    <xsl:sequence select="rng:nsName"/>
  </xsl:variable>

  <xsl:variable name="prefix" as="xs:string?">
    <xsl:choose>
      <xsl:when test="$nsName/@ns = 'http://www.w3.org/1999/xhtml'">
        <xsl:sequence select="'h:'"/>
      </xsl:when>
      <xsl:when test="$nsName/@ns = 'http://xmlcalabash.com/ns/dot'">
        <xsl:sequence select="'dot:'"/>
      </xsl:when>
      <xsl:otherwise/>
    </xsl:choose>
  </xsl:variable>

  <ss:attribute name="{$prefix}{{any-name}}" optional="{$repeat}">
    <xsl:attribute name="type">
      <xsl:choose>
	<xsl:when test="$content/rng:data">
	  <xsl:apply-templates select="$content"/>
	</xsl:when>
	<xsl:when test="$content/rng:oneOrMore/rng:data">
	  <xsl:apply-templates select="$content"/>
          <xsl:text>+</xsl:text>
	</xsl:when>
	<xsl:when test="$content/rng:zeroOrMore/rng:data">
	  <xsl:apply-templates select="$content"/>
          <xsl:text>*</xsl:text>
	</xsl:when>
	<xsl:when test="$content/rng:ref">
	  <xsl:variable name="pattern" select="$content/rng:ref/@name"/>
	  <xsl:variable name="rngpat" select="$schema/rng:define[@name=$pattern]"/>
	  <xsl:choose>
	    <xsl:when test="$rngpat/@sa:model">
	      <xsl:value-of select="$rngpat/@sa:model"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:message>
		<xsl:text>Warning: unsupported ref in attribute: </xsl:text>
		<xsl:value-of select="@name"/>
                <xsl:text>: </xsl:text>
                <xsl:value-of select="$content/rng:ref/@name"/>
	      </xsl:message>
	    </xsl:otherwise>
	  </xsl:choose>
	</xsl:when>
	<xsl:when test="$content/rng:choice/rng:value">
	  <xsl:for-each select="$content/rng:choice/rng:value|$content/rng:choice/rng:data">
	    <xsl:if test="position()&gt;1">|</xsl:if>
	    <xsl:choose>
	      <xsl:when test="self::rng:value">
		<xsl:value-of select="."/>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:text>xs:</xsl:text>
		<xsl:value-of select="@type"/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </xsl:for-each>
	</xsl:when>
	<xsl:when test="$content/rng:value">
	  <xsl:text>"</xsl:text>
	  <xsl:value-of select="$content/rng:value"/>
	  <xsl:text>"</xsl:text>
	</xsl:when>
	<xsl:when test="$content/rng:text or not($content/*)">
	  <xsl:text>string</xsl:text>
	</xsl:when>
	<xsl:when test="rng:text or $nsName or not(* except (db:purpose|rng:anyName))">
	  <xsl:text>string</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:message>
	    <xsl:text>Warning: unsupported content in attribute: name=</xsl:text>
	    <xsl:value-of select="@name"/>
	    <xsl:text> (</xsl:text>
	    <xsl:value-of select="ancestor::rng:element/@name"/>
	    <xsl:text>)</xsl:text>
          </xsl:message>
          <xsl:message select="$content"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <xsl:sequence select="db:purpose/node()"/>
  </ss:attribute>
</xsl:template>

<xsl:template match="rng:data">
  <xsl:value-of select="@type"/>
</xsl:template>

<xsl:template match="rng:empty"/>

<xsl:template match="rng:text">
  <ss:model name="string" repeat=""/>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="ss:element-summary">
  <xsl:param name="namespace" as="xs:string?" tunnel="yes"/>
  <xsl:param name="suppress-prefix" as="xs:string?" tunnel="yes"/>

  <p id="{if (@xml:id) then @xml:id else generate-id(.)}">
    <!-- this generates the prefix= attribute ...
    <xsl:sequence select="f:html-attributes(., generate-id(.))"/>
    -->
    <xsl:attribute name="class">
      <xsl:text>element-syntax</xsl:text>
      <xsl:if test="@class">
	<xsl:text> element-syntax-</xsl:text>
	<xsl:value-of select="@class"/>
      </xsl:if>
    </xsl:attribute>
    <code>
      <xsl:text>&lt;</xsl:text>

      <xsl:choose>
	<xsl:when test="@name != ''">
          <xsl:if test="not($suppress-prefix = 'true')">
	    <xsl:value-of select="@prefix"/>
	    <xsl:text>:</xsl:text>
          </xsl:if>
	  <xsl:value-of select="if (contains(@name,':'))
				then substring-after(@name,':')
				else @name"/>
          <xsl:if test="@namespace">
            <xsl:choose>
              <xsl:when test="$suppress-prefix = 'true'">
                <xsl:text> xmlns</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text> xmlns:</xsl:text>
                <xsl:value-of select="@prefix"/>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:text>="</xsl:text>
            <xsl:value-of select="@namespace"/>
            <xsl:text>"</xsl:text>
          </xsl:if>
	</xsl:when>
	<xsl:when test=".//ss:model[@name='subpipeline']">
	  <var>
	    <xsl:value-of select="@prefix"/>
	    <xsl:text>:compound-step</xsl:text>
	  </var>
	</xsl:when>
	<xsl:otherwise>
	  <var>
	    <xsl:value-of select="if (@prefix != '') then @prefix || ':' else ''"/>
	    <xsl:text>{any-name}</xsl:text>
	  </var>
	</xsl:otherwise>
      </xsl:choose>

      <xsl:apply-templates select="ss:attribute"/>

      <xsl:choose>
	<xsl:when test="*[not(self::ss:attribute)]">
	  <xsl:text>&gt;</xsl:text>
	  <br/>
	  <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
	  <xsl:apply-templates select="*[not(self::ss:attribute)]"/>
	  <xsl:text>&lt;/</xsl:text>

	  <xsl:choose>
	    <xsl:when test="@name != ''">
	      <xsl:value-of select="@prefix"/>
	      <xsl:text>:</xsl:text>
	      <xsl:value-of select="if (contains(@name,':'))
				    then substring-after(@name,':')
				    else @name"/>
	    </xsl:when>
	    <xsl:when test=".//ss:model[@name='subpipeline']">
	      <var>
		<xsl:value-of select="@prefix"/>
		<xsl:text>:compound-step</xsl:text>
	      </var>
	    </xsl:when>
	    <xsl:otherwise>
	      <var>
		<xsl:value-of select="@prefix"/>
		<xsl:text>:atomic-step</xsl:text>
	      </var>
	    </xsl:otherwise>
	  </xsl:choose>

	  <xsl:text>&gt;</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:text>&#160;/&gt;</xsl:text>
	</xsl:otherwise>
      </xsl:choose>
    </code>
  </p>
</xsl:template>

<!-- This is an awful, one-off hack because I'm on a train
     and this was not the bug I wanted to be working on today. -->
<xsl:template match="ss:attribute[starts-with(@name, 'h:')
                                  and count(../ss:attribute[starts-with(@name, 'h:')]) gt 1]"
              priority="10">
  <xsl:if test="empty(preceding-sibling::ss:attribute[starts-with(@name, 'h:')])">
    <br/>
    <xsl:text>&#160;&#160;</xsl:text>
    <xsl:text>h:*</xsl:text>
    <xsl:text> = string</xsl:text>
  </xsl:if>
</xsl:template>

<!-- This is an awful, one-off hack because I'm on a train
     and this was not the bug I wanted to be working on today. -->
<xsl:template match="ss:attribute[starts-with(@name, 'dot:')
                                  and count(../ss:attribute[starts-with(@name, 'dot:')]) gt 1]"
              priority="10">
  <xsl:if test="empty(preceding-sibling::ss:attribute[starts-with(@name, 'dot:')])">
    <br/>
    <xsl:text>&#160;&#160;</xsl:text>
    <xsl:text>dot:*</xsl:text>
    <xsl:text> = string</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="ss:attribute">
  <br/>
  <xsl:text>&#160;&#160;</xsl:text>
  <xsl:choose>
    <xsl:when test="@optional = ''">
      <strong>
	<xsl:value-of select="@name"/>
      </strong>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="@name"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:value-of select="@optional"/>
  <xsl:text> = </xsl:text>

  <xsl:if test="@avt = 'true'">
    <xsl:text>{ </xsl:text>
  </xsl:if>

  <!-- hack! -->
  <xsl:choose>
    <xsl:when test="contains(@type,'&quot;')">
      <xsl:value-of select="@type"/>
    </xsl:when>
    <xsl:when test="@type = 'QName'">
      <var>EQName</var>
    </xsl:when>
    <xsl:otherwise>
      <var>
	<xsl:value-of select="@type"/>
      </var>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="@avt = 'true'">
    <xsl:text> }</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="ss:content-model">
  <xsl:apply-templates/>
  <br/>
</xsl:template>

<xsl:template match="ss:group">
  <xsl:choose>
    <xsl:when test="count(*) &gt; 1">
      <xsl:text>(</xsl:text>
      <xsl:apply-templates/>
      <xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="count(*) &gt; 0">
    <xsl:value-of select="@repeat"/>
    <xsl:call-template name="separator"/>
  </xsl:if>
</xsl:template>

<xsl:template match="ss:model">
  <var>
    <xsl:value-of select="@name"/>
  </var>
  <xsl:value-of select="@repeat"/>
  <xsl:call-template name="separator"/>
</xsl:template>

<xsl:template match="ss:element" mode="table">
  <xsl:text>&#160;&#160;</xsl:text>
  <xsl:apply-templates select="."/>
</xsl:template>

<xsl:template match="ss:element">
  <xsl:param name="prefix" tunnel="yes"/>

  <!-- This is an awful, one-off hack because I'm on a train
       and this was not the bug I wanted to be working on today. -->
  <xsl:variable name="idpfx"
                select="if (@name = 'h:td')
                        then 'h.'
                        else $prefix || '.'"/>

  <xsl:variable name="basename" select="if (contains(@name,':'))
			                then substring-after(@name,':')
			                else if (@name = '{any-name}')
                                             then 'any-name'
                                             else @name"/>

  <xsl:choose>
    <xsl:when test="$idpfx = ''">
      <var>
	<xsl:value-of select="$basename"/>
	<xsl:value-of select="@repeat"/>
      </var>
    </xsl:when>
    <xsl:when test="@name = '{any-name}'">
      <a href="#{$idpfx}{$basename}">
	<xsl:value-of select="$basename"/>
      </a>
      <xsl:value-of select="@repeat"/>
    </xsl:when>
    <xsl:otherwise>
      <a href="#{$idpfx}{$basename}">
	<xsl:value-of select="@name"/>
      </a>
      <xsl:value-of select="@repeat"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:call-template name="separator"/>
</xsl:template>

<xsl:template name="separator">
  <xsl:choose>
    <xsl:when test="not(following-sibling::*)"/>
    <xsl:when test="parent::ss:group[@type='choice']">
      <xsl:text> | </xsl:text>
      <br/>
      <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
      <xsl:for-each select="ancestor::ss:group">&#160;</xsl:for-each>
    </xsl:when>
    <xsl:when test="parent::ss:group[@type='interleave']">
      <xsl:text> &amp; </xsl:text>
      <br/>
      <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
      <xsl:for-each select="ancestor::ss:group">&#160;</xsl:for-each>
    </xsl:when>
    <xsl:when test="parent::ss:group[@type='sequence']|parent::ss:content-model">
      <xsl:text>,</xsl:text>
      <br/>
      <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
      <xsl:for-each select="ancestor::ss:group">&#160;</xsl:for-each>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="ss:element-summary" mode="table">
  <div id="{if (@xml:id) then @xml:id else generate-id(.)}"
       class="element-syntax-table">
    <table>
      <tbody>
        <tr>
          <td class="code first">
            <xsl:text>&lt;</xsl:text>

            <xsl:choose>
	      <xsl:when test="@name != ''">
	        <xsl:value-of select="@prefix"/>
	        <xsl:text>:</xsl:text>
	        <xsl:value-of select="if (contains(@name,':'))
				      then substring-after(@name,':')
				      else @name"/>
	      </xsl:when>
	      <xsl:when test=".//ss:model[@name='subpipeline']">
	        <var>
	          <xsl:value-of select="@prefix"/>
	          <xsl:text>:compound-step</xsl:text>
	        </var>
	      </xsl:when>
	      <xsl:otherwise>
	        <var>
	          <xsl:value-of select="@prefix"/>
	          <xsl:text>:atomic-step</xsl:text>
	        </var>
	      </xsl:otherwise>
            </xsl:choose>

            <xsl:if test="empty(ss:attribute)">&gt;</xsl:if>
            
          </td>
          <td class="desc"></td>
        </tr>

        <xsl:apply-templates select="ss:attribute" mode="table"/>

        <xsl:choose>
	  <xsl:when test="*[not(self::ss:attribute)]">
            <tr>
              <xsl:if test="exists(ss:attribute)">
                <td class="code">
	          <xsl:text>&gt;</xsl:text>
                </td>
              </xsl:if>
              <td class="desc"></td>
            </tr>
	    <xsl:apply-templates select="*[not(self::ss:attribute)]" mode="table"/>
            <tr>
              <td class="code last">
	        <xsl:text>&lt;/</xsl:text>
	        <xsl:choose>
	          <xsl:when test="@name != ''">
	            <xsl:value-of select="@prefix"/>
	            <xsl:text>:</xsl:text>
	            <xsl:value-of select="if (contains(@name,':'))
				          then substring-after(@name,':')
				          else @name"/>
	          </xsl:when>
	          <xsl:when test=".//ss:model[@name='subpipeline']">
	            <var>
		      <xsl:value-of select="@prefix"/>
		      <xsl:text>:compound-step</xsl:text>
	            </var>
	          </xsl:when>
	          <xsl:otherwise>
	            <var>
		      <xsl:value-of select="@prefix"/>
		      <xsl:text>:atomic-step</xsl:text>
	            </var>
	          </xsl:otherwise>
	        </xsl:choose>

	        <xsl:text>&gt;</xsl:text>
              </td>
              <td class="desc"></td>
            </tr>
	  </xsl:when>
	  <xsl:otherwise>
            <tr>
              <td class="code last">
	        <xsl:text>&#160;/&gt;</xsl:text>
              </td>
              <td class="desc"></td>
            </tr>
	  </xsl:otherwise>
        </xsl:choose>
      </tbody>
    </table>
  </div>
</xsl:template>

<xsl:template match="ss:content-model" mode="table">
  <tr>
    <td class="code">
      <xsl:apply-templates mode="table"/>
    </td>
    <td>
    </td>
  </tr>
</xsl:template>

<xsl:template match="ss:attribute" mode="table">
  <tr>
    <td class="code">
      <xsl:text>&#160;&#160;</xsl:text>
      <xsl:choose>
        <xsl:when test="@optional = ''">
          <strong>
	    <xsl:value-of select="@name"/>
          </strong>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:value-of select="@optional"/>
      <xsl:text> = </xsl:text>

      <xsl:if test="@avt = 'true'">
        <xsl:text>{ </xsl:text>
      </xsl:if>

      <!-- hack! -->
      <xsl:choose>
        <xsl:when test="contains(@type,'&quot;')">
          <xsl:value-of select="@type"/>
        </xsl:when>
        <xsl:when test="@type = 'QName'">
          <var>EQName</var>
        </xsl:when>
        <xsl:otherwise>
          <var>
	    <xsl:value-of select="@type"/>
          </var>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:if test="@avt = 'true'">
        <xsl:text> }</xsl:text>
      </xsl:if>
    </td>
    <td class="desc">
      <xsl:sequence select="node()"/>
    </td>
  </tr>
</xsl:template>

</xsl:stylesheet>
