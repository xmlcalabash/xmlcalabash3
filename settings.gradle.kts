plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "xmlcalabash"

include("xmlcalabash")
include("app")
include("test-driver")
include("documentation")
include("paged-media:antenna-house")
include("paged-media:fop")
include("paged-media:prince")
include("paged-media:weasyprint")
include("send-mail")
include("template:java")
include("template:kotlin")
include("ext:asciidoctor")
include("ext:cache")
include("ext:collection-manager")
include("ext:diagramming")
include("ext:ebnf-convert")
include("ext:epubcheck")
include("ext:find")
include("ext:json-patch")
include("ext:jsonpath")
include("ext:markup-blitz")
include("ext:metadata-extractor")
include("ext:pipeline-messages")
include("ext:polyglot")
include("ext:railroad")
include("ext:rdf")
include("ext:selenium")
include("ext:trang")
include("ext:unique-id")
include("ext:wait-for-update")
include("ext:xmlunit")
include("ext:xpath")
