import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

val dep_fop = project.findProperty("fop").toString()

dependencies {
  implementation(project(":xmlcalabash"))

  implementation("org.apache.xmlgraphics:fop:${dep_fop}")
  implementation("org.apache.avalon.framework:avalon-framework-api:4.3.1")
  implementation("org.apache.avalon.framework:avalon-framework-impl:4.3.1")
  implementation("javax.media:jai-core:1.1.3")
  implementation("com.sun.media:jai-codec:1.1.3")
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
