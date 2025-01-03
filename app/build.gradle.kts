import com.xmlcalabash.build.XmlCalabashBuildExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import java.net.URI
import java.net.URL

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.9.20")
  }
}

plugins {
  id("buildlogic.kotlin-application-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  id("org.jetbrains.dokka") version "1.9.20"
}

val xmlcalabashRelease by configurations.dependencyScope("xmlcalabashRelease")
val sendmailRelease by configurations.dependencyScope("sendmailRelease")
val weasyprintRelease by configurations.dependencyScope("weasyprintRelease")
val princeRelease by configurations.dependencyScope("princeRelease")
val antennahouseRelease by configurations.dependencyScope("antennahouseRelease")
val fopRelease by configurations.dependencyScope("fopRelease")
val uniqueidRelease by configurations.dependencyScope("uniqueidRelease")
val metadataextractorRelease by configurations.dependencyScope("metadataextractorRelease")
val cacheRelease by configurations.dependencyScope("cacheRelease")
val xpathRelease by configurations.dependencyScope("xpathRelease")
val pipelineMessagesRelease by configurations.dependencyScope("pipelineMessagesRelease")

val dep_slf4j = project.findProperty("slf4j").toString()

dependencies {
  xmlcalabashRelease(project(mapOf("path" to ":xmlcalabash",
                                   "configuration" to "releaseArtifacts")))
  sendmailRelease(project(mapOf("path" to ":send-mail",
                                "configuration" to "releaseArtifacts")))
  weasyprintRelease(project(mapOf("path" to ":paged-media:weasyprint",
                                  "configuration" to "releaseArtifacts")))
  princeRelease(project(mapOf("path" to ":paged-media:prince",
                              "configuration" to "releaseArtifacts")))
  antennahouseRelease(project(mapOf("path" to ":paged-media:antenna-house",
                                    "configuration" to "releaseArtifacts")))
  fopRelease(project(mapOf("path" to ":paged-media:fop",
                           "configuration" to "releaseArtifacts")))
  uniqueidRelease(project(mapOf("path" to ":ext:unique-id",
                                "configuration" to "releaseArtifacts")))
  metadataextractorRelease(project(mapOf("path" to ":ext:metadata-extractor",
                                         "configuration" to "releaseArtifacts")))
  cacheRelease(project(mapOf("path" to ":ext:cache",
                             "configuration" to "releaseArtifacts")))
  xpathRelease(project(mapOf("path" to ":ext:xpath",
                             "configuration" to "releaseArtifacts")))
  pipelineMessagesRelease(project(mapOf("path" to ":ext:pipeline-messages",
                                        "configuration" to "releaseArtifacts")))

  implementation(project(":xmlcalabash"))
  implementation(project(":send-mail"))
  implementation(project(":paged-media:antenna-house"))
  implementation(project(":paged-media:prince"))
  implementation(project(":paged-media:weasyprint"))
  implementation(project(":paged-media:fop"))
  implementation(project(":ext:unique-id"))
  implementation(project(":ext:metadata-extractor"))
  implementation(project(":ext:cache"))
  implementation(project(":ext:xpath"))
  implementation(project(":ext:pipeline-messages"))

  implementation("org.slf4j:slf4j-api:${dep_slf4j}")
}

val xmlcalabashJar = configurations.resolvable("xmlcalabashJar") {
  extendsFrom(xmlcalabashRelease)
}
val sendmailJar = configurations.resolvable("sendmailJar") {
  extendsFrom(sendmailRelease)
}
val weasyprintJar = configurations.resolvable("weasyprintJar") {
  extendsFrom(weasyprintRelease)
}
val princeJar = configurations.resolvable("princeJar") {
  extendsFrom(princeRelease)
}
val antennahouseJar = configurations.resolvable("antennahouseJar") {
  extendsFrom(antennahouseRelease)
}
val fopJar = configurations.resolvable("fopJar") {
  extendsFrom(fopRelease)
}
val uniqueidJar = configurations.resolvable("uniqueidJar") {
  extendsFrom(uniqueidRelease)
}
val metadataextractorJar = configurations.resolvable("metadataextractorJar") {
  extendsFrom(metadataextractorRelease)
}
val cacheJar = configurations.resolvable("cacheJar") {
  extendsFrom(cacheRelease)
}
val xpathJar = configurations.resolvable("xpathJar") {
  extendsFrom(xpathRelease)
}
val pipelineMessagesJar = configurations.resolvable("pipelineMessagesJar") {
  extendsFrom(pipelineMessagesRelease)
}

application {
  // Define the main class for the application.
  mainClass = "com.xmlcalabash.app.Main"
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.withType<DokkaTask>().configureEach {
  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    customStyleSheets = listOf(file("../documentation/src/dokka/resources/css/xmlcalabash.css"))
    //templatesDir = file("../documentation/src/dokka/resources/templates")
    footerMessage = "© 2024 Norm Tovey-Walsh"
    separateInheritedMembers = false
    mergeImplicitExpectActualDeclarations = false
  }

  dokkaSourceSets {
    named("main") {
      moduleName.set("XML Calabash")
      includes.from("Module.md")
      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl.set(URI("https://github.com/xmlcalabash3/xmlcalabash").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

fun distClasspath(): List<File> {
  val libs = mutableListOf<File>()
  configurations["distributionClasspath"].forEach {
    if (it.isFile() && !it.getName().startsWith("Saxon-EE")) {
      libs.add(it)
    }
  }
  return libs
}

tasks.jar {
  val libs = mutableListOf<String>()
  for (jar in distClasspath()) {
    if (jar.getName().startsWith("Saxon-HE")) {
      libs.add("lib/Saxon-EE-${project.findProperty("saxonVersion")}.jar")
      libs.add("lib/Saxon-PE-${project.findProperty("saxonVersion")}.jar")
    }
    libs.add("lib/${jar.getName()}")
  }

  archiveFileName.set("xmlcalabash-${xmlbuild.jarArchiveFilename()}")
  manifest {
    attributes("Project-Name" to "XML Calabash",
               "Main-Class" to "com.xmlcalabash.app.Main",
               "Class-Path" to libs.joinToString(" "))
  }
}

val copyScripts = tasks.register<Copy>("copyScripts") {
  inputs.file(layout.projectDirectory.file("src/main/scripts/xmlcalabash.sh"))
  outputs.file(layout.buildDirectory.file("stage/xmlcalabash.sh"))
  from(layout.projectDirectory.dir("src/main/scripts"))
  into(layout.buildDirectory.dir("stage"))
  include("xmlcalabash.sh")
  filter { line ->
    line.replace("@@VERSION@@", xmlbuild.version.get())
  }
}

tasks.register("stage-release") {
  inputs.files(xmlcalabashJar)
  inputs.files(sendmailJar)
  inputs.files(weasyprintJar)
  inputs.files(princeJar)
  inputs.files(antennahouseJar)
  inputs.files(fopJar)
  inputs.files(uniqueidJar)
  inputs.files(metadataextractorJar)
  inputs.files(cacheJar)
  inputs.files(xpathJar)
  inputs.files(pipelineMessagesJar)
  inputs.files(copyScripts)
  dependsOn("jar")

  doLast {
    mkdir(layout.buildDirectory.dir("stage"))
    mkdir(layout.buildDirectory.dir("stage/lib"))
    copy {
      from(layout.buildDirectory.dir("libs"))
      into(layout.buildDirectory.dir("stage"))
    }
  }

  doLast {
    distClasspath().forEach { path ->
      copy {
        from(path)
        into(layout.buildDirectory.dir("stage/lib"))
        exclude("META-INF/**")
        exclude("com/**")
      }                      
    }
  }

  doLast {
    copy {
      from(layout.projectDirectory.dir("src/main/docs"))
      into(layout.buildDirectory.dir("stage"))
      filter { line ->
        line.replace("@@VERSION@@", xmlbuild.version.get())
      }
    }
  }

  doLast {
    copy {
      from(xmlcalabashJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(sendmailJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(weasyprintJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(princeJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(antennahouseJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(fopJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(uniqueidJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(metadataextractorJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(cacheJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(xpathJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
  doLast {
    copy {
      from(pipelineMessagesJar)
      into(layout.buildDirectory.dir("stage/lib"))
    }
  }
}

tasks.register<Zip>("release") {
  dependsOn("stage-release")
  from(layout.buildDirectory.dir("stage"))
  into("xmlcalabash-${xmlbuild.version.get()}/")
  archiveFileName = "xmlcalabash-${xmlbuild.version.get()}.zip"
}

tasks.register("helloWorld") {
  doLast {
    println("Building with Java version ${System.getProperty("java.version")}")
    for (jar in distClasspath()) {
      println("APP: ${jar}")
    }
  }
}

