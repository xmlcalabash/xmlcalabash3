import com.xmlcalabash.build.ExternalDependencies
import com.xmlcalabash.build.XmlCalabashBuildExtension
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
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
  application
}

val xmlbuild = the<XmlCalabashBuildExtension>()

val xmlcalabashRelease by configurations.dependencyScope("xmlcalabashRelease")

val dep_activation = project.findProperty("activation").toString()
val dep_drewnoakesExtractor = project.findProperty("drewnoakesExtractor").toString()
val dep_jaxbapi = project.findProperty("jaxbapi").toString()
val dep_nineml = project.findProperty("nineml").toString()
val dep_pdfbox = project.findProperty("pdfbox").toString()
val dep_slf4j = project.findProperty("slf4j").toString()

dependencies {
  xmlcalabashRelease(project(mapOf("path" to ":xmlcalabash",
                                   "configuration" to "releaseArtifacts")))

  implementation(project(":xmlcalabash"))
  //implementation(project(":ext:polyglot")) // No, it requires Java 17
}

val xmlcalabashJar = configurations.resolvable("xmlcalabashJar") {
  extendsFrom(xmlcalabashRelease)
}

application {
  // Define the main class for the application.
  mainClass = "com.xmlcalabash.app.Main"
}

tasks.withType<DokkaTaskPartial>().configureEach {
  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    customStyleSheets = listOf(file("../documentation/src/dokka/resources/css/xmlcalabash.css"))
    //templatesDir = file("../documentation/src/dokka/resources/templates")
    footerMessage = "© 2024-2025 Norm Tovey-Walsh"
    separateInheritedMembers = false
    mergeImplicitExpectActualDeclarations = false
  }

  dokkaSourceSets {
    named("main") {
      documentedVisibilities.set(setOf(Visibility.PUBLIC))
      moduleName.set("XML Calabash Application")
      includes.from("Module.md")
      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl.set(URI("https://github.com/xmlcalabash/xmlcalabash3").toURL())
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
  inputs.file(layout.projectDirectory.file("src/main/scripts/xmlcalabash.ps1"))
  outputs.file(layout.buildDirectory.file("stage/xmlcalabash.sh"))
  outputs.file(layout.buildDirectory.file("stage/xmlcalabash.ps1"))

  doFirst {
    // Never let different versions get co-staged
    delete(layout.buildDirectory.dir("stage"))
  }

  from(layout.projectDirectory.dir("src/main/scripts"))
  into(layout.buildDirectory.dir("stage"))
  include("xmlcalabash.sh")
  include("xmlcalabash.ps1")
  filter { line ->
    line.replace("@@VERSION@@", xmlbuild.version.get())
  }
}

tasks.register("stage-release") {
  inputs.files(xmlcalabashJar)
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
    listOf(xmlcalabashJar).forEach { jar ->
      copy {
        from(jar)
        into(layout.buildDirectory.dir("stage/lib"))
      }
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
    println(ExternalDependencies.of(listOf("metadata-extractor", "nineml")))
/*
    println("Building with Java version ${System.getProperty("java.version")}")
    for (jar in distClasspath()) {
      println("APP: ${jar}")
    }
*/
  }
}

