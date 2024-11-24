import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

dependencies {
  implementation(project(":xmlcalabash"))

  implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
