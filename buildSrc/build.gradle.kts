plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
  implementation(libs.kotlin.gradle.plugin)
  implementation("nu.validator.htmlparser:htmlparser:1.4")
  implementation("net.sf.saxon:Saxon-HE:${project.findProperty("saxonVersion")}")

  // These aren't needed here; but if I don't include them, the documentation
  // tasks don't seem to find or load the xslTNG classes so...¯\_(ツ)_/¯
  implementation("org.docbook:schemas-docbook:5.2")
  implementation("org.docbook:docbook-xslTNG:${project.findProperty("xslTNGversion")}")
}

gradlePlugin {
  plugins {
    create("xmlcalabash-build") {
      id = "com.xmlcalabash.build.xmlcalabash-build"
      implementationClass = "com.xmlcalabash.build.XmlCalabashBuildPlugin"
    }
  }
}
