import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

dependencies {
  implementation(project(":xmlcalabash"))

  implementation("org.nineml:coffeegrinder:1.99.7")
  implementation("org.nineml:coffeefilter:1.99.7")
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}
