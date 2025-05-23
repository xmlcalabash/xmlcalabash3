import com.xmlcalabash.build.ExternalDependencies

plugins {
    id("buildlogic.kotlin-common-conventions")
    `java-library`
}

dependencies {
  ExternalDependencies.of(listOf("xmlcalabash")).forEach {
    implementation(it) {
      exclude(group="net.sf.saxon", module="Saxon-HE")
    }
  }
  implementation(files("lib/"))

  ExternalDependencies.of(listOf("asciidoctor", "diagramming", "ebnf-convert",
                                 "epubcheck", "find", "json-patch", "jsonpath",
                                 "markup-blitz", "metadata-extractor", "railroad", "rdf",
                                 "selenium", "send-mail", "trang", "unique-id", "xmlunit")).forEach {
    compileOnly(it) {
      exclude(group="net.sf.saxon", module="Saxon-HE")
    }
    testImplementation(it) {
      exclude(group="net.sf.saxon", module="Saxon-HE")
    }
  }

  // For testing sendmail
  testImplementation("org.json:json:20240303")
}
