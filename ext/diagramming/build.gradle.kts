import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

val dep_plantuml = project.findProperty("plantUml").toString()
val dep_jeuclid = project.findProperty("jeuclidCore").toString()
val dep_ditaa = project.findProperty("ditaa").toString().replace(".", "_")

dependencies {
  implementation(project(":xmlcalabash"))

  implementation("net.sourceforge.plantuml:plantuml:${dep_plantuml}")
  implementation("de.rototor.jeuclid:jeuclid-core:${dep_jeuclid}")
  implementation(files(layout.projectDirectory.file("lib/ditaa${dep_ditaa}.jar")))
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
