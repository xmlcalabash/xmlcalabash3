import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

val dep_jing = project.findProperty("jing").toString()

dependencies {
  implementation(project(":xmlcalabash"))
  implementation("org.relaxng:trang:${dep_jing}") {
    exclude(group="net.sf.saxon", module="Saxon-HE")
  }
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
