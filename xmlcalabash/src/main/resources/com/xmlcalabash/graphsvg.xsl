<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                expand-text="yes"
                version="3.0">
<xsl:import href="https://xmlcalabash.com/xsl/static.xsl"/>

<xsl:output method="html" html-version="5" encoding="utf-8"/>

<xsl:param name="version" as="xs:string" select="'«unknown»'"/>
<xsl:param name="basename" as="xs:string" select="'«unknown»'"/>
<xsl:param name="output" as="xs:string" select="'«unknown»'"/>

<xsl:template match="/">
  <html class="graph">
    <head>
      <meta charset="UTF-8"/>
      <title>{$basename} {$output}</title>
      <script src="data:application/javascript;base64,{replace($graphsvg.js, '&#10;', '')}" defer="defer">
      </script>
      <xsl:sequence select="$css"/>
      </head>
      <body>
        <p>
          <img class="__logo" src="data:image/gif;base64,{replace($logo, '&#10;', '')}"/>
          <span class="__scale"> </span>
          Visualization: <code><a href="../index.html">{$output}s</a>/{$basename}</code>
        </p>
        <div class="svg">
          <img><xsl:sequence select="/"/></img>
        </div>
      </body>
    </html>
</xsl:template>

</xsl:stylesheet>
