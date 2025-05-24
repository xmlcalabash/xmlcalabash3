plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "xmlcalabash"

include("xmlcalabash")
include("app")
include("test-driver")
include("documentation")
include("template:java")
include("template:kotlin")
include("ext:polyglot")
