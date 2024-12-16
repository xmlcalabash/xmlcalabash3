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
val coffeepressRelease by configurations.dependencyScope("coffeepressRelease")
val sendmailRelease by configurations.dependencyScope("sendmailRelease")
val weasyprintRelease by configurations.dependencyScope("weasyprintRelease")
val princeRelease by configurations.dependencyScope("princeRelease")
val antennahouseRelease by configurations.dependencyScope("antennahouseRelease")
val fopRelease by configurations.dependencyScope("fopRelease")
val uniqueidRelease by configurations.dependencyScope("uniqueidRelease")
val metadataextractorRelease by configurations.dependencyScope("metadataextractorRelease")
val cacheRelease by configurations.dependencyScope("cacheRelease")
//val polyglotRelease by configurations.dependencyScope("polyglotRelease")

dependencies {
  xmlcalabashRelease(project(mapOf("path" to ":xmlcalabash",
                                   "configuration" to "releaseArtifacts")))
  coffeepressRelease(project(mapOf("path" to ":ixml-coffeepress",
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
//  polyglotRelease(project(mapOf("path" to ":ext:polyglot",
//                                "configuration" to "releaseArtifacts")))

  implementation(project(":xmlcalabash"))
  implementation(project(":ixml-coffeepress"))
  implementation(project(":send-mail"))
  implementation(project(":paged-media:antenna-house"))
  implementation(project(":paged-media:prince"))
  implementation(project(":paged-media:weasyprint"))
  implementation(project(":paged-media:fop"))
  implementation(project(":ext:unique-id"))
  implementation(project(":ext:metadata-extractor"))
  implementation(project(":ext:cache"))
//  implementation(project(":ext:polyglot"))
}

val xmlcalabashJar = configurations.resolvable("xmlcalabashJar") {
  extendsFrom(xmlcalabashRelease)
}
val coffeepressJar = configurations.resolvable("coffeepressJar") {
  extendsFrom(coffeepressRelease)
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
//val polyglotJar = configurations.resolvable("polyglotJar") {
//  extendsFrom(polyglotRelease)
//}

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
    libs.add("lib/${jar.getName()}")
  }

  archiveFileName.set("xmlcalabash-${xmlbuild.jarArchiveFilename()}")
  manifest {
    attributes("Project-Name" to "XML Calabash",
               "Main-Class" to "com.xmlcalabash.app.Main",
               "Class-Path" to libs.joinToString(" "))
  }
}

tasks.register("stage-release") {
  inputs.files(xmlcalabashJar)
  inputs.files(coffeepressJar)
  inputs.files(sendmailJar)
  inputs.files(weasyprintJar)
  inputs.files(princeJar)
  inputs.files(antennahouseJar)
  inputs.files(fopJar)
  inputs.files(uniqueidJar)
  inputs.files(metadataextractorJar)
  inputs.files(cacheJar)
//  inputs.files(polyglotJar)
  dependsOn("jar")

  doLast {
    println("Staging release...")
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
        exclude("META-INF/ **")
        exclude("com/ **")
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
      from(coffeepressJar)
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
//  doLast {
//    copy {
//      from(polyglotJar)
//      into(layout.buildDirectory.dir("stage/lib"))
//    }
//  }
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
  }
}

