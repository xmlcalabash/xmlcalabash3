import java.nio.file.*
import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  id("org.graalvm.buildtools.native") version "0.10.2"
  id("maven-publish")
  id("signing")
}

val xmlcalabash by configurations.creating {}

configurations.forEach {
  it.exclude("net.sf.saxon.Saxon-HE")
}

val dep_graalvmJS = project.findProperty("graalvmJS").toString()

dependencies {
  implementation(project(":xmlcalabash"))
  implementation("org.graalvm.polyglot:polyglot:${dep_graalvmJS}")
  implementation("org.graalvm.polyglot:js:${dep_graalvmJS}")
  implementation("org.graalvm.polyglot:python:${dep_graalvmJS}")
  xmlcalabash(project(":xmlcalabash"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

val xmlbuild = the<XmlCalabashBuildExtension>()

val isGraalVM = Files.exists(Paths.get("${System.getProperty("java.home")}/lib/graalvm"))
val jvmDefaultArgs = if (isGraalVM) {
  // None required...
  listOf<String>()
} else {
  configurations {
    create("compilerClasspath") {
      isCanBeResolved = true
    }
  }
  
  dependencies {
    "compilerClasspath"("org.graalvm.compiler:compiler:${dep_graalvmJS}")
  }
  
  val compilerDependencies = configurations.getByName("compilerClasspath")
      .filter { it.name.endsWith(".jar") }  // Filter out POMs

  listOf("-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI",
         "--upgrade-module-path=${compilerDependencies.asPath}")
}

if (isGraalVM) {
  println("Building with GraalVM Java version ${System.getProperty("java.version")}")
} else {
  println("Building with Java version ${System.getProperty("java.version")}")
}

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
}

tasks.withType<Test> {
  jvmArgs(jvmDefaultArgs)
}

tasks.register("stage-release") {
  dependsOn("jar")

  doLast {
    mkdir(layout.buildDirectory.dir("stage"))
    mkdir(layout.buildDirectory.dir("stage/extra"))
    copy {
      from(layout.buildDirectory.dir("libs"))
      into(layout.buildDirectory.dir("stage/extra"))
    }
  }

  doLast {
    xmlbuild.distClasspath(configurations["xmlcalabash"], configurations["distributionClasspath"])
        .forEach { path ->
          copy {
            from(path)
            into(layout.buildDirectory.dir("stage/extra"))
            exclude("META-INF/**")
            exclude("com/**")
          }                      
        }
  }

  doLast {
    copy {
      from(layout.projectDirectory.dir("src/main/docs"))
      into(layout.buildDirectory.dir("stage"))
      filter { line ->
        line.replace("@@VERSION@@", xmlbuild.version.get())
      }
    }
  }

  doLast {
    copy {
      from(layout.projectDirectory)
      into(layout.buildDirectory.dir("stage"))
      include("README.md")
      filter { line ->
        line.replace("Polyglot extension step",
                     "Polyglot extension step version ${xmlbuild.version.get()}")
            .replace("<version>", xmlbuild.version.get())
      }
    }
  }
}

tasks.register<Zip>("release") {
  dependsOn("stage-release")
  from(layout.buildDirectory.dir("stage"))
  into("${xmlbuild.name.get()}-${xmlbuild.version.get()}/")
  archiveFileName = "${xmlbuild.name.get()}-${xmlbuild.version.get()}.zip"
}

publishing {
  repositories {
    maven {
      credentials {
        username = project.findProperty("sonatypeUsername").toString()
        password = project.findProperty("sonatypePassword").toString()
      }
      url = if (xmlbuild.version.get().contains("SNAPSHOT")) {
        uri("https://oss.sonatype.org/content/repositories/snapshots/")
      } else {
        uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
      }
    }
  }

  publications {
    create<MavenPublication>("mavenPolyglot") {
      pom {
        groupId = project.findProperty("xmlcalabashGroup").toString()
        version = project.findProperty("xmlcalabashVersion").toString()
        name = "XML Calabash Polyglot Step"
        packaging = "jar"
        description = "An polyglot step for XML Calabash 3.x"
        url = "https://github.com/xmlcalabash/xmlcalabash3"

        scm {
          url = "scm:git@github.com:xmlcalabash/xmlcalabash3.git"
          connection = "scm:git@github.com:xmlcalabash/xmlcalabash3.git"
          developerConnection = "scm:git@github.com:xmlcalabash/xmlcalabash3.git"
        }

        licenses {
          license {
            name = "MIT License"
            url = "http://www.opensource.org/licenses/mit-license.php"
            distribution = "repo"
          }
        }

        developers {
          developer {
            id = "ndw"
            name = "Norm Tovey-Walsh"
          }
        }
      }

      from(components["java"])
      artifact(sourcesJar.get())
    }
  }
}

signing {
  sign(publishing.publications["mavenPolyglot"])
}
