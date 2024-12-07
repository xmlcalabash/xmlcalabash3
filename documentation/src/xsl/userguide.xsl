<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:mp="http://docbook.org/ns/docbook/modes/private"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:rddl="http://www.rddl.org/"
                xmlns:t="http://docbook.org/ns/docbook/templates"
                xmlns:tp="http://docbook.org/ns/docbook/templates/private"
                xmlns:v="http://docbook.org/ns/docbook/variables"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<!--
<xsl:import href="file:///Volumes/Projects/docbook/xslTNG/build/xslt/docbook.xsl"/>
-->
<xsl:import href="https://cdn.docbook.org/release/xsltng/current/xslt/docbook.xsl"/>

<xsl:import href="common.xsl"/>
<xsl:import href="rngsyntax.xsl"/>
<xsl:import href="user-custom.xsl"/>

<xsl:param name="dep_schxslt" select="'UNCONFIGURED'"/>
<xsl:param name="dep_htmlparser" select="'UNCONFIGURED'"/>
<xsl:param name="dep_commonsCodec" select="'UNCONFIGURED'"/>
<xsl:param name="dep_commonsCompress" select="'UNCONFIGURED'"/>
<xsl:param name="dep_brotliDec" select="'UNCONFIGURED'"/>
<xsl:param name="dep_tukaaniXz" select="'UNCONFIGURED'"/>
<xsl:param name="dep_flexmarkAll" select="'UNCONFIGURED'"/>
<xsl:param name="dep_uuidCreator" select="'UNCONFIGURED'"/>
<xsl:param name="dep_jsonSchemaValidator" select="'UNCONFIGURED'"/>
<xsl:param name="dep_graalvmJS" select="'UNCONFIGURED'"/>

</xsl:stylesheet>
