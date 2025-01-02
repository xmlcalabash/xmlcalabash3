import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

val dep_drewnoakes = project.findProperty("drewnoakesExtractor").toString()
val dep_pdfbox = project.findProperty("pdfbox").toString()
val dep_jaxb = project.findProperty("jaxbapi").toString()

dependencies {
  implementation(project(":xmlcalabash"))

  implementation("com.drewnoakes:metadata-extractor:${dep_drewnoakes}")
  implementation("org.apache.pdfbox:pdfbox:${dep_pdfbox}")
  implementation("org.apache.pdfbox:xmpbox:${dep_pdfbox}")
  implementation("javax.xml.bind:jaxb-api:${dep_jaxb}") // pdfbox dep for parsing XML in pdfbox
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
