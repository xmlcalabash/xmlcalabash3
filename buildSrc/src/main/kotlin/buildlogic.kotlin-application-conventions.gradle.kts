import com.xmlcalabash.build.ExternalDependencies

plugins {
    id("buildlogic.kotlin-common-conventions")
}

dependencies {
  ExternalDependencies.of(listOf("xmlcalabash", "asciidoctor", "diagramming", "ebnf-convert",
                                 "epubcheck", "find", "json-patch", "jsonpath",
                                 "markup-blitz", "metadata-extractor", "railroad", "rdf",
                                 "selenium", "send-mail", "trang", "unique-id", "xmlunit")).forEach {
    implementation(it) {
      exclude(group="net.sf.saxon", module="Saxon-HE")
    }
  }
}
