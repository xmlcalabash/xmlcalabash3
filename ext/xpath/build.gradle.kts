import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  `maven-publish`
}

val xmlcalabash by configurations.creating {}

configurations.forEach {
  it.exclude("net.sf.saxon.Saxon-HE")
}

dependencies {
  implementation(project(":xmlcalabash"))
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
}

tasks.register("helloWorld") {
  doLast {
    println(xmlbuild.jarArchiveFilename())
  }
}
