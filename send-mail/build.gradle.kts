import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

dependencies {
  implementation(project(":xmlcalabash"))

  implementation("javax.activation:activation:1.1.1")
  implementation("javax.mail:javax.mail-api:1.6.2")
  implementation("com.sun.mail:javax.mail:1.6.2")

  testImplementation("org.json:json:20240303")
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
