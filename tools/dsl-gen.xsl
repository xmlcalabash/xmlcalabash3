<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:f="http://nwalsh.com/ns/functions"
                expand-text="yes"
                version="3.0">

<xsl:output method="text" encoding="utf-8" indent="no"/>

<xsl:key name="element" match="elements/element" use="@name"/>

<xsl:param name="package" as="xs:string" select="'com.xmlcalabash.xslt'"/>
<xsl:param name="vocab" as="xs:string" select="'xslt'"/>
<xsl:param name="prefix" as="xs:string" select="'xsl'"/>
<xsl:param name="uri" as="xs:string" select="'http://www.w3.org/1999/XSL/Transform'"/>
<xsl:param name="entry" as="xs:string" select="'stylesheet'"/>

<xsl:variable name="n" select="'&#10;'"/>

<xsl:template match="elements">
  <xsl:text>package {$package}

import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.*
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.type.BuiltInAtomicType
import java.net.URI

interface {f:cn($vocab)}ElementInterface {{
    fun render(builder: SaxonTreeBuilder)
}}

class {f:cn($vocab)}TextElement(val text: String) : {f:cn($vocab)}ElementInterface {{
    override fun render(builder: SaxonTreeBuilder) {{
        builder.addText(text)
    }}
}}

@DslMarker
annotation class {f:cn($vocab)}TagMarker

@{f:cn($vocab)}TagMarker
abstract class {f:cn($vocab)}Tag(val prefix: String, val namespace: String, val localName: String) : {f:cn($vocab)}ElementInterface {{
    val name = QName(prefix, namespace, localName)
    val children = arrayListOf&lt;{f:cn($vocab)}ElementInterface&gt;()
    val attributes = hashMapOf&lt;QName, String&gt;()
    val namespaces = hashMapOf&lt;String, String&gt;()

    protected fun &lt;T: {f:cn($vocab)}ElementInterface&gt; initTag(tag: T, init: T.() -&gt; Unit): T {{
        tag.init()
        children.add(tag)
        return tag
    }}

    override fun render(builder: SaxonTreeBuilder) {{
        builder.addStartElement(name, attributeMap(), namespaceMap())
        for (c in children) {{
            c.render(builder)
        }}
        builder.addEndElement()
    }}

    fun maybe(name: String, value: String?) {{
        if (value != null) {{
            attributes[QName(name)] = value
        }}
    }}

    fun maybe(name: String, value: Boolean?) {{
        if (value != null) {{
            attributes[QName(name)] = value.toString()
        }}
    }}

    private fun attributeMap(): AttributeMap {
        var attrs: AttributeMap = EmptyAttributeMap.getInstance()
        for ((name, value) in attributes) {
            attrs = attrs.put(
                AttributeInfo(
                FingerprintedQName(name.prefix, name.namespaceUri, name.localName),
                BuiltInAtomicType.UNTYPED_ATOMIC,
                value,
                null,
                ReceiverOption.NONE
            ))
        }
        return attrs
    }

    private fun namespaceMap(): NamespaceMap {{
        var nsmap = NamespaceMap.emptyMap()
        for ((prefix, uri) in namespaces) {{
            nsmap = nsmap.put(prefix, NamespaceUri.of(uri))
        }}
        nsmap = nsmap.put(prefix, NamespaceUri.of(namespace))
        return nsmap
    }}
}}
</xsl:text>

  <xsl:apply-templates select="element" mode="class"/>
  <xsl:apply-templates select="element[@name = $entry]">
    <xsl:with-param name="inner" select="false()"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="element" mode="class">
  <xsl:text>class {f:cn($vocab)}{f:cn(@name)}(val vPrefix: String, val vNamespace: String): {f:cn($vocab)}Tag(vPrefix, vNamespace, "{@name}")</xsl:text>
  <xsl:choose>
    <xsl:when test="group/element or @text-content='true'">
      <xsl:text> {{{$n}</xsl:text>
      <xsl:if test="@text-content='true'">
        <text>    operator fun String.unaryPlus() {{{$n}</text>
        <text>        children.add(XsltTextElement(this)){$n}</text>
        <text>    }}{$n}</text>
      </xsl:if>
      <xsl:apply-templates select="group/element"/>
      <xsl:text>}}{$n}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>{$n}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>{$n}</xsl:text>
  <xsl:text>{$n}</xsl:text>
</xsl:template>

<xsl:template match="element">
  <xsl:param name="inner" as="xs:boolean" select="true()"/>
  <xsl:variable name="indent" select="if ($inner) then '    ' else ''"/>

  <xsl:text>{$indent}fun {f:mn(@name)}({$n}</xsl:text>
  <xsl:if test="not($inner)">    builder: SaxonTreeBuilder,{$n}</xsl:if>
  <xsl:if test="not($inner)">    documentUri: String? = null,{$n}</xsl:if>
  <xsl:choose>
    <xsl:when test="$inner">
      <xsl:text>        vocabularyPrefix: String = vPrefix,{$n}</xsl:text>
      <xsl:text>        vocabularyNamespace: String = vNamespace,{$n}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>    vocabularyPrefix: String = "{/*/@prefix}",{$n}</xsl:text>
      <xsl:text>    vocabularyNamespace: String = "{/*/@namespace}",{$n}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates select="key('element', @name)/attribute" mode="params">
    <xsl:with-param name="inner" select="$inner"/>
  </xsl:apply-templates>
  <xsl:text>{$indent}    attributes: Map&lt;QName,String&gt; = mapOf(),{$n}</xsl:text>
  <xsl:text>{$indent}    ns: Map&lt;String,String&gt; = mapOf(),{$n}</xsl:text>
  <xsl:choose>
    <xsl:when test="$inner">
      <xsl:text>{$indent}    init: {f:cn($vocab)}{f:cn(@name)}.() -&gt; Unit) {{{$n}</xsl:text>
      <xsl:text>{$indent}    val tag = initTag({f:cn($vocab)}{f:cn(@name)}(vocabularyPrefix, vocabularyNamespace), init){$n}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>{$indent}    init: {f:cn($vocab)}{f:cn(@name)}.() -&gt; Unit): XdmNode {{{$n}</xsl:text>
      <xsl:text>{$indent}    val tag = {f:cn($vocab)}{f:cn(@name)}(vocabularyPrefix, vocabularyNamespace){$n}</xsl:text>
      <xsl:text>{$indent}    tag.init(){$n}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>{$indent}    tag.attributes.putAll(attributes){$n}</xsl:text>
  <xsl:text>{$indent}    tag.namespaces.putAll(ns){$n}</xsl:text>
  <xsl:apply-templates select="key('element', @name)/attribute" mode="values">
    <xsl:with-param name="inner" select="$inner"/>
  </xsl:apply-templates>
  <xsl:if test="not($inner)">
    <xsl:text>  builder.startDocument(if (documentUri == null) null else URI(documentUri)){$n}</xsl:text>
    <xsl:text>    tag.render(builder){$n}</xsl:text>
    <xsl:text>    builder.endDocument(){$n}</xsl:text>
    <xsl:text>    return builder.result{$n}</xsl:text>
  </xsl:if>
  <xsl:text>{$indent}}}{$n}</xsl:text>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="attribute" mode="params">
  <xsl:param name="inner" as="xs:boolean" select="true()"/>
  <xsl:variable name="indent" select="if ($inner) then '    ' else ''"/>

  <xsl:text>{$indent}    {f:mn(@name)}: </xsl:text>
  <xsl:choose>
    <xsl:when test="@type = 'boolean'">
      <xsl:text>Boolean</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>String</xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
    <xsl:when test="@default">
      <xsl:text> = "{@default/string()}"</xsl:text>
    </xsl:when>
    <xsl:when test="@optional = 'true'">
      <xsl:text>? = </xsl:text>
      <xsl:choose>
        <xsl:when test="@default">
          <xsl:text>"{@default}"</xsl:text>
        </xsl:when>
        <xsl:when test="@optional = 'true'">
          <xsl:text>null</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <!-- nop -->
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>,{$n}</xsl:text>
</xsl:template>

<xsl:template match="attribute" mode="values">
  <xsl:param name="inner" as="xs:boolean" select="true()"/>
  <xsl:variable name="indent" select="if ($inner) then '    ' else ''"/>
  <xsl:choose>
    <xsl:when test="@optional = 'true'">
      <xsl:text>{$indent}    tag.maybe("{@name}", {f:mn(@name)}){$n}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>{$indent}    tag.attributes[QName("{@name}")] = {f:mn(@name)}{$n}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="attribute[value]" mode="values">
  <xsl:param name="inner" as="xs:boolean" select="true()"/>
  <xsl:variable name="indent" select="if ($inner) then '    ' else ''"/>

  <xsl:variable name="name" select="f:mn(@name)"/>
  <xsl:text>{$indent}    if ({$name} != null</xsl:text>
  <xsl:for-each select="value">
    <xsl:text>{$n}{$indent}        &amp;&amp; {$name} != "{.}"</xsl:text> 
  </xsl:for-each>
  <xsl:text>) {{{$n}</xsl:text>
  <xsl:text>{$indent}        throw RuntimeException("Invalid {$name}: ${$name}"){$n}</xsl:text>
  <xsl:text>{$indent}    }}{$n}</xsl:text>
  <xsl:next-match>
    <xsl:with-param name="inner" select="$inner"/>
  </xsl:next-match>
</xsl:template>

<!-- ============================================================ -->

<xsl:variable name="reserved" select="('if', 'for', 'try', 'break', 'when', 'package', 'as')"/>

<xsl:function name="f:upperFirst" as="xs:string">
  <xsl:param name="name" as="xs:string"/>
  <xsl:sequence select="upper-case(substring($name, 1, 1))||substring($name,2)"/>
</xsl:function>

<xsl:function name="f:cn" as="xs:string">
  <xsl:param name="name" as="xs:string"/>
  <xsl:variable name="parts" as="xs:string+" select="tokenize($name, '-')"/>
  <xsl:variable name="result" select="string-join($parts ! f:upperFirst(.), '')"/>
  <xsl:choose>
    <xsl:when test="$result = $reserved">
      <xsl:sequence select="'`'||$result||'`'"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$result"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<xsl:function name="f:mn" as="xs:string">
  <xsl:param name="name" as="xs:string"/>
  <xsl:variable name="parts" as="xs:string+" select="tokenize($name, '-')"/>
  <xsl:variable name="result" select="string-join(($parts[1], $parts[position() gt 1] ! f:upperFirst(.)), '')"/>
  <xsl:choose>
    <xsl:when test="$result = $reserved">
      <xsl:sequence select="'`'||$result||'`'"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$result"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

</xsl:stylesheet>
