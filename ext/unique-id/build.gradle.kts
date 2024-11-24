import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

dependencies {
  implementation(project(":xmlcalabash"))
  implementation("com.github.f4b6a3:uuid-creator:6.0.0")
  implementation("com.github.f4b6a3:ulid-creator:5.2.3")
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
