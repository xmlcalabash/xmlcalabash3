import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  id("maven-publish")
  id("signing")
}

val xmlcalabash by configurations.creating {}

configurations.forEach {
  it.exclude("net.sf.saxon.Saxon-HE")
}

val dep_epubcheck = project.findProperty("epubcheck").toString()
val dep_activation = project.findProperty("activation").toString()

dependencies {
  implementation(project(":xmlcalabash"))
  implementation("org.w3c:epubcheck:${dep_epubcheck}")
  implementation("javax.activation:activation:${dep_activation}") // For mimetype mapping

  xmlcalabash(project(":xmlcalabash"))
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
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
        line.replace("EPUBCheck extension step",
                     "EPUBCheck extension step version ${xmlbuild.version.get()}")
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
    create<MavenPublication>("mavenEpubCheck") {
      pom {
        groupId = project.findProperty("xmlcalabashGroup").toString()
        version = project.findProperty("xmlcalabashVersion").toString()
        name = "XML Calabash EPUBCheck Step"
        packaging = "jar"
        description = "An EPUBCheck step for XML Calabash 3.x"
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
  sign(publishing.publications["mavenEpubCheck"])
}
