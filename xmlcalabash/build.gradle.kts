import com.xmlcalabash.build.XmlCalabashBuildExtension

import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

import java.net.URI
import java.net.URL
import java.io.PrintStream

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.9.20")
  }
}

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  id("com.github.gmazzo.buildconfig") version "5.5.0"
  id("org.jetbrains.dokka") version "1.9.20"
  id("maven-publish")
  id("signing")
}

configurations.forEach {
  it.exclude("com.sun.xml.ibind.jaxp")
  it.exclude("isorelax")
  it.exclude("relaxngDatatype")
  it.exclude("net.sf.saxon.Saxon-HE")
}

// If you add to this list, update BuildConfig as well!
val dep_activation = project.findProperty("activation").toString()
val dep_brotliDec = project.findProperty("brotliDec").toString()
val dep_commonsCodec = project.findProperty("commonsCodec").toString()
val dep_commonsCompress = project.findProperty("commonsCompress").toString()
val dep_ditaa = project.findProperty("ditaa").toString()
val dep_drewnoakesExtractor = project.findProperty("drewnoakesExtractor").toString()
val dep_epubcheck = project.findProperty("epubcheck").toString()
val dep_flexmarkAll = project.findProperty("flexmarkAll").toString()
val dep_graalvmJS = project.findProperty("graalvmJS").toString()
val dep_htmlparser = project.findProperty("htmlparser").toString()
val dep_httpClient = project.findProperty("httpClient").toString()
val dep_jacksonDataformat = project.findProperty("jacksonDataformat").toString()
val dep_jaxbapi = project.findProperty("jaxbapi").toString()
val dep_jeuclid = project.findProperty("jeuclidCore").toString()
val dep_jing = project.findProperty("jing").toString()
val dep_jsonSchemaValidator = project.findProperty("jsonSchemaValidator").toString()
val dep_markupBlitz = project.findProperty("markupBlitz").toString()
val dep_pdfbox = project.findProperty("pdfbox").toString()
val dep_plantuml = project.findProperty("plantUml").toString()
val dep_rr = project.findProperty("rr").toString()
val dep_schxslt2 = project.findProperty("schxslt2").toString()
val dep_sinclude = project.findProperty("sinclude").toString()
val dep_slf4j = project.findProperty("slf4j").toString()
val dep_tukaaniXz = project.findProperty("tukaaniXz").toString()
val dep_uuidCreator = project.findProperty("uuidCreator").toString()
val dep_xerces = project.findProperty("xercesImpl").toString()
val dep_xmlResolver = project.findProperty("xmlResolver").toString()
val dep_blitz = project.findProperty("markupBlitz").toString()
val dep_nineml = project.findProperty("nineml").toString()

dependencies {
  // Compile with Markup Blitz but only ship NineML
  // Is that the right answer?
  compileOnly("de.bottlecaps:markup-blitz:${dep_blitz}")
  implementation("org.nineml:coffeegrinder:${dep_nineml}")
  implementation("org.nineml:coffeefilter:${dep_nineml}")

  implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")

  // implementation("name.dmaus.schxslt:schxslt:${dep_schxslt}")
  // The SchXslt2 transpiler is included in our resources

  implementation("nu.validator:htmlparser:${dep_htmlparser}")
  implementation("commons-codec:commons-codec:${dep_commonsCodec}")

  implementation("org.apache.commons:commons-compress:${dep_commonsCompress}")
  implementation("org.brotli:dec:${dep_brotliDec}")
  implementation("org.tukaani:xz:${dep_tukaaniXz}")

  implementation("javax.activation:activation:${dep_activation}") // For mimetype mapping
  implementation("com.nwalsh:sinclude:${dep_sinclude}") {
    exclude(group="net.sf.saxon", module="Saxon-HE")
  }
  implementation("com.vladsch.flexmark:flexmark-all:${dep_flexmarkAll}")
  implementation("com.github.f4b6a3:uuid-creator:${dep_uuidCreator}")
  implementation("org.relaxng:jing:${dep_jing}") {
    exclude(group="net.sf.saxon", module="Saxon-HE")
  }
  implementation("xerces:xercesImpl:${dep_xerces}")
  implementation("com.networknt:json-schema-validator:${dep_jsonSchemaValidator}")
  //implementation("org.graalvm.polyglot:js:${dep_graalvmJS}")
  implementation("org.xmlresolver:xmlresolver:${dep_xmlResolver}")

  // I was using log4j but httpclient5 uses slf4j.
  // Could I get httpclient5 to use log4j? Maybe. ¯\_(ツ)_/¯
  // But I got tired of trying to figure it out so I did this instead.
  implementation("org.slf4j:slf4j-api:${dep_slf4j}")
  implementation("ch.qos.logback:logback-classic:1.5.13")
  implementation("org.apache.logging.log4j:log4j-to-slf4j:2.23.1")

  implementation("org.apache.httpcomponents.client5:httpclient5:${dep_httpClient}")

  implementation("org.jline:jline-terminal:3.28.0")
  implementation("org.jline:jline-terminal-jni:3.28.0")
  implementation("org.jline:jline-reader:3.28.0")

  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${dep_jacksonDataformat}")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:${dep_jacksonDataformat}")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:${dep_jacksonDataformat}")

  implementation(files("lib/"))
}

val xmlbuild = the<XmlCalabashBuildExtension>()

buildConfig {
  className("XmlCalabashBuildConfig")
  packageName("com.xmlcalabash")
  useKotlinOutput { internalVisibility = false } 

  buildConfigField("NAME", xmlbuild.name.get())
  buildConfigField("VERSION", xmlbuild.version.get())
  buildConfigField("PRODUCT_NAME", xmlbuild.productName.get())
  buildConfigField("VENDOR_NAME", xmlbuild.vendorName.get())
  buildConfigField("VENDOR_URI", xmlbuild.vendorUri.get())
  buildConfigField("BUILD_DATE", xmlbuild.buildDate.get())
  buildConfigField("BUILD_ID", xmlbuild.buildId())
  buildConfigField("SCHXSLT2", project.findProperty("schxslt2").toString())

  val sb = StringBuilder()
  sb.append("mapOf(\n")
  sb.append("  \"saxon\" to \"${project.findProperty("saxonVersion").toString()}\",\n")
  arrayOf<String>("activation",
                  "asciidoctorj",
                  "asciidoctorjPdf",
                  "brotliDec",
                  "commonsCodec",
                  "commonsCompress",
                  "ditaa",
                  "drewnoakesExtractor",
                  "ebnfConvert",
                  "epubcheck",
                  "flexmarkAll",
                  "fop",
                  "graalvmJS",
                  "htmlparser",
                  "httpClient",
                  "jaxbapi",
                  "jena",
                  "jeuclidCore",
                  "jing",
                  "jsonSchemaValidator",
                  "markupBlitz",
                  "nineml",
                  "pdfbox",
                  "plantUml",
                  "rr",
                  "schxslt2",
                  "semargl",
                  "selenium",
                  "sinclude",
                  "slf4j",
                  "tukaaniXz",
                  "uuidCreator",
                  "xercesImpl",
                  "xmlResolver",
                  "xmlunit"
  ).forEach { dep ->
    sb.append("  \"").append(dep).append("\" to ")
    sb.append("\"").append(project.findProperty(dep).toString()).append("\"")
    if (dep != "xmlunit") {
      sb.append(",")
    }
    sb.append("\n")
  }
  sb.append(")\n")
  
  buildConfigField("java.util.Map<String,String>", "DEPENDENCIES", sb.toString())
}

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
}

tasks.withType<DokkaTaskPartial>().configureEach {
  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    customStyleSheets = listOf(file("../documentation/src/dokka/resources/css/xmlcalabash.css"))
    //templatesDir = file("../documentation/src/dokka/resources/templates")
    footerMessage = "© 2024 Norm Tovey-Walsh"
    separateInheritedMembers = false
    mergeImplicitExpectActualDeclarations = false
  }

  dokkaSourceSets {
    named("main") {
      documentedVisibilities.set(setOf(Visibility.PUBLIC, Visibility.PROTECTED))
      moduleName.set("XMLCalabash")
      includes.from("Module.md")
      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl.set(URI("https://github.com/xmlcalabash/xmlcalabash3").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

tasks.register("apidocs") {
  dependsOn("dokkaJavadoc")
  doLast {
    val stream = PrintStream(layout.buildDirectory.file("dokka/javadoc/details.json").get().asFile)
    stream.println("{\"version\": \"${xmlbuild.version.get()}\", \"pubdate\": \"${xmlbuild.buildTime.get()}\"}")
    stream.close()
  }
}

publishing {
  repositories {
    maven {
      credentials {
        username = project.findProperty("sonatypeUsername").toString()
        password = project.findProperty("sonatypePassword").toString()
      }
      url = if (xmlbuild.version.get().contains("SNAPSHOT")) {
        uri("https://oss.sonatype.org/content/repositories/snapshots/")
      } else {
        uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
      }
    }
  }

  publications {
    create<MavenPublication>("mavenXmlCalabash") {
      pom {
        groupId = project.findProperty("xmlcalabashGroup").toString()
        version = project.findProperty("xmlcalabashVersion").toString()
        name = "XML Calabash"
        packaging = "jar"
        description = "An XProc 3.0 processor"
        url = "https://github.com/xmlcalabash/xmlcalabash3"

        scm {
          url = "scm:git@github.com:xmlcalabash/xmlcalabash3.git"
          connection = "scm:git@github.com:xmlcalabash/xmlcalabash3.git"
          developerConnection = "scm:git@github.com:xmlcalabash/xmlcalabash3.git"
        }

        licenses {
          license {
            name = "MIT License"
            url = "http://www.opensource.org/licenses/mit-license.php"
            distribution = "repo"
          }
        }

        developers {
          developer {
            id = "ndw"
            name = "Norm Tovey-Walsh"
          }
        }
      }

      from(components["java"])
      artifact(sourcesJar.get())
    }
  }
}

signing {
  sign(publishing.publications["mavenXmlCalabash"])
}

