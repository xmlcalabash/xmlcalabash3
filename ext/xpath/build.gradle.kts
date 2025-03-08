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

dependencies {
  implementation(project(":xmlcalabash"))
}

val xmlbuild = the<XmlCalabashBuildExtension>()

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
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
    create<MavenPublication>("mavenXPath") {
      pom {
        groupId = project.findProperty("xmlcalabashGroup").toString()
        version = project.findProperty("xmlcalabashVersion").toString()
        name = "XML Calabash XPath Step"
        packaging = "jar"
        description = "XPath step for XML Calabash 3.x"
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
  sign(publishing.publications["mavenXPath"])
}
