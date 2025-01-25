import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  `maven-publish`
}

val xmlcalabash by configurations.creating {}

configurations.forEach {
  it.exclude("net.sf.saxon.Saxon-HE")
}

val dep_plantuml = project.findProperty("plantUml").toString()
val dep_jeuclid = project.findProperty("jeuclidCore").toString()
val dep_ditaa = project.findProperty("ditaa").toString().replace(".", "_")

dependencies {
  implementation(project(":xmlcalabash"))
  implementation("net.sourceforge.plantuml:plantuml:${dep_plantuml}")
  implementation("de.rototor.jeuclid:jeuclid-core:${dep_jeuclid}")
  implementation(files(layout.projectDirectory.file("lib/ditaa${dep_ditaa}.jar")))

  xmlcalabash(project(":xmlcalabash"))
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
  from(zipTree(layout.projectDirectory.file("lib/ditaa0_9.jar"))) {
      exclude("org/apache/commons/cli/**")
  }
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

  // We've re-jarred the ditaa classes into the diagramming jar file because
  // it's not available in Maven; so don't include it in the extras...
  doLast {
    xmlbuild.distClasspath(configurations["xmlcalabash"], configurations["distributionClasspath"])
        .filter { path ->
          path.name != "ditaa${dep_ditaa}.jar"
        }
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
        line.replace("Diagramming extension steps",
                     "Diagramming extension steps version ${xmlbuild.version.get()}")
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
    create<MavenPublication>("mavenXmlCalabash") {
      pom {
        groupId = project.findProperty("xmlcalabashGroup").toString()
        version = project.findProperty("xmlcalabashVersion").toString()
        name = "XML Calabash Diagramming Steps"
        packaging = "jar"
        description = "Diagramming steps for XML Calabash 3.x"
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
