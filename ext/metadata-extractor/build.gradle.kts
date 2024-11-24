import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

dependencies {
  implementation(project(":xmlcalabash"))

  implementation("com.drewnoakes:metadata-extractor:2.19.0")
  implementation("org.apache.pdfbox:pdfbox:2.0.31")
  implementation("org.apache.pdfbox:xmpbox:2.0.31")
  implementation("javax.xml.bind:jaxb-api:2.3.1") // pdfbox dep for parsing XML in pdfbox
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
