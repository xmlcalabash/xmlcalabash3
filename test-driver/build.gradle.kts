import java.io.PrintStream
import com.xmlcalabash.build.XmlCalabashBuildExtension

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

repositories {
  mavenLocal()
  mavenCentral()
  maven { url = uri("https://maven.saxonica.com/maven") }
}

val saxonVersion = project.properties["saxonVersion"].toString()
val requirePass = project.findProperty("requirePass")?.toString() ?: "true"
val consoleOutput = project.findProperty("xmlcalabash.testDriver.consoleOutput")?.toString() ?: "false"

val transformation by configurations.creating
val testrunner by configurations.creating {
  extendsFrom(configurations["runtimeClasspath"])
}

dependencies {
  implementation(project(":xmlcalabash"))
  implementation(project(":send-mail"))
  implementation(project(":paged-media:antenna-house"))
  implementation(project(":paged-media:prince"))
  implementation(project(":paged-media:weasyprint"))
  implementation(project(":paged-media:fop"))
  implementation(project(":ext:unique-id"))
  implementation(project(":ext:metadata-extractor"))
  implementation(project(":ext:pipeline-messages"))
  implementation(project(":ext:cache"))
  implementation(project(":ext:epubcheck"))
  implementation(project(":ext:xpath"))
  implementation(project(":ext:polyglot"))
  implementation(project(":ext:wait-for-update"))
  implementation(project(":ext:diagramming"))
  implementation(project(":ext:collection-manager"))
  implementation(files("lib"))
  // WTF? Why is this necessary?
  implementation(files("build/classes/kotlin/main"))
  implementation(project(":ext:asciidoctor"))
  implementation(project(":ext:railroad"))
  implementation(project(":ext:rdf"))
  implementation(project(":ext:xmlunit"))
  implementation(project(":ext:markup-blitz"))
  implementation(project(":ext:selenium"))
  implementation(project(":ext:ebnf-convert"))
  implementation(project(":ext:trang"))
  implementation(project(":ext:json-patch"))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

  transformation ("net.sf.saxon:Saxon-HE:${saxonVersion}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

val xmlbuild = the<XmlCalabashBuildExtension>()

if (project.findProperty("testPattern") == null) {
  tasks.register("run-test") {
    doLast {
      println("Configure test with -PtestPattern and -PtestDir")
    }
  }
} else {
  tasks.register<JavaExec>("run-test") {
    val testPattern = project.findProperty("testPattern").toString()
    val testDir = project.findProperty("testDir")?.toString()
        ?: "../tests/3.0-test-suite/test-suite/tests"

    classpath = configurations.named("testrunner").get()
    mainClass = "com.xmlcalabash.testdriver.Main"

    inputs.dir(layout.projectDirectory.dir(testDir))
    inputs.file(layout.projectDirectory.file("src/test/resources/exclusions.txt"))

    args("-t:${testPattern}")
  }
}

tasks.register<JavaExec>("test-suite") {
  dependsOn("build")

  classpath = configurations.named("testrunner").get()
  mainClass = "com.xmlcalabash.testdriver.Main"

  inputs.dir(layout.projectDirectory.dir("../tests/3.0-test-suite"))
  inputs.file(layout.projectDirectory.file("src/test/resources/exclusions.txt"))
  outputs.file(layout.buildDirectory.file("test-suite-results.xml"))

  args("--title:XProc 3.0 Test Suite",
       "--require-pass:${requirePass}",
       "--console:${consoleOutput}",
       "--dir:${layout.projectDirectory.dir("../tests/3.0-test-suite/test-suite/tests")}",
       "--report:${layout.buildDirectory.file("test-suite-results.xml").get().asFile}")
}

tasks.register<JavaExec>("extra-suite") {
  dependsOn("build")

  classpath = configurations.named("testrunner").get()
  mainClass = "com.xmlcalabash.testdriver.Main"

  inputs.dir(layout.projectDirectory.dir("../tests/extra-suite"))
  inputs.file(layout.projectDirectory.file("src/test/resources/exclusions.txt"))
  outputs.file(layout.buildDirectory.file("extra-suite-results.xml"))

  args("--title:XML Calabash Extra Test Suite",
       "--require-pass:${requirePass}",
       "--console:${consoleOutput}",
       "--dir:${layout.projectDirectory.dir("../tests/extra-suite/test-suite/tests")}",
       "--report:${layout.buildDirectory.file("extra-suite-results.xml").get().asFile}")
}

tasks.register<JavaExec>("selenium") {
  dependsOn("build")

  classpath = configurations.named("testrunner").get()
  mainClass = "com.xmlcalabash.testdriver.Main"

  inputs.dir(layout.projectDirectory.dir("../tests/selenium"))
  inputs.file(layout.projectDirectory.file("src/test/resources/exclusions.txt"))
  outputs.file(layout.buildDirectory.file("selenium-results.xml"))

  args("--title:XML Calabash Selenium Test Suite",
       "--require-pass:${requirePass}",
       "--console:${consoleOutput}",
       "--dir:${layout.projectDirectory.dir("../tests/selenium/test-suite/tests")}",
       "--report:${layout.buildDirectory.file("selenium-results.xml").get().asFile}")
}

tasks.register<JavaExec>("test-report") {
  dependsOn("test-suite")
  dependsOn("extra-suite")
  dependsOn("selenium")
  dependsOn("copy-extra")

  classpath = configurations.named("transformation").get()
  mainClass = "net.sf.saxon.Transform"

  inputs.file(layout.buildDirectory.file("test-suite-results.xml"))
  inputs.file(layout.buildDirectory.file("extra-suite-results.xml"))
  inputs.file(layout.buildDirectory.file("selenium-results.xml"))
  inputs.file(layout.projectDirectory.file("src/xsl/test-report.xsl"))
  outputs.file(layout.buildDirectory.file("test-report/index.html"))
  outputs.file(layout.buildDirectory.file("test-report/version.json"))
  
  args("-s:${layout.buildDirectory.file("test-suite-results.xml").get().asFile}",
       "-xsl:${layout.projectDirectory.file("src/xsl/test-report.xsl")}",
       "-o:${layout.buildDirectory.file("test-report/index.html").get().asFile}")

  doLast {
    val output = layout.buildDirectory.file("test-report/version.json").get().asFile
    val stream = PrintStream(output)
    stream.println("{")
    stream.println("\"version\": \"${xmlbuild.version.get()}\"")
    stream.println("}")
    stream.close()
  }
}

// Doesn't run the most time consuming tests...
tasks.register("quick-report") {
  dependsOn("test-suite")
  dependsOn("extra-suite")
  doLast {
    println("Report finished")
  }
}

tasks.register("copy-extra") {
  inputs.dir(layout.projectDirectory.dir("src/css"))
  inputs.dir(layout.projectDirectory.dir("src/js"))
  outputs.dir(layout.buildDirectory.dir("test-report"))

  doLast {
    copy {
      into(layout.buildDirectory.dir("test-report"))
      from(layout.projectDirectory.file("src/css"))
    }
  }
  doLast {
    copy {
      into(layout.buildDirectory.dir("test-report"))
      from(layout.projectDirectory.file("src/js"))
    }
  }
}
