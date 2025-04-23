import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URI
import java.net.URL

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.9.20")
  }
}

repositories {
    mavenLocal()
    mavenCentral()
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "1.9.20"
}

// You can apply and configure Dokka in each subproject
// individially or configure all subprojects at once
/*
subprojects {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            documentedVisibilities.set(setOf(
                Visibility.PUBLIC,
                Visibility.PROTECTED
            ))

            // Read docs for more details: https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
            sourceLink {
                val exampleDir = "https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example"

                localDirectory.set(rootProject.projectDir)
                remoteUrl.set(URI("$exampleDir").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}
*/

// Configures only the parent MultiModule task,
// this will not affect subprojects
tasks.dokkaHtmlMultiModule {
    moduleName.set("XML Calabash 3.x")
/*
    removeChildTasks(
        listOf(project(":ext"), project(":paged-media"))
    )  
*/
}
