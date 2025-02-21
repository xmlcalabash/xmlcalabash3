plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "xmlcalabash"

include("xmlcalabash", "app", "test-driver",
        "documentation",
        "send-mail",
        "paged-media:weasyprint", "paged-media:prince",
        "paged-media:antenna-house", "paged-media:fop",
        "ext:unique-id", "ext:metadata-extractor", "ext:cache",
        "ext:pipeline-messages", "ext:epubcheck",
        "ext:polyglot", "ext:xpath", "ext:wait-for-update",
        "ext:diagramming", "ext:collection-manager",
        "ext:asciidoctor",
        "template:kotlin", "template:java",
        "ext:rdf", "ext:railroad", "ext:xmlunit"
    )
