plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/") }
    maven { url = uri("https://maven.saxonica.com/maven") }
}

val distributionClasspath by configurations.creating {
  extendsFrom(configurations["runtimeClasspath"])
}

val releaseArtifacts by configurations.consumable("releaseArtifacts")

dependencies {
  //implementation("net.sf.saxon:Saxon-HE:12.5")
  implementation("com.saxonica:Saxon-EE:${project.findProperty("saxonVersion")}")
  implementation("org.apache.logging.log4j:log4j-api-kotlin:1.4.0")

  distributionClasspath("net.sf.saxon:Saxon-HE:${project.findProperty("saxonVersion")}")

  constraints {
    implementation("net.sf.saxon:Saxon-HE") {
      version {
        strictly ("${project.findProperty("saxonVersion")}")
      }
    }
  }

  constraints {
    implementation("com.saxonica:Saxon-EE") {
      version {
        strictly ("${project.findProperty("saxonVersion")}")
      }
    }
  }

  testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

artifacts {
  add(releaseArtifacts.name, tasks.named("jar"))
}
