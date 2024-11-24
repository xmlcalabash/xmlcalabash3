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
}

gradlePlugin {
  plugins {
    create("xmlcalabash-build") {
      id = "com.xmlcalabash.build.xmlcalabash-build"
      implementationClass = "com.xmlcalabash.build.XmlCalabashBuildPlugin"
    }
  }
}
