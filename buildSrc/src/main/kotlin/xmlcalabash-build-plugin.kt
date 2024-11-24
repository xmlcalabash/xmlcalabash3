package com.xmlcalabash.build

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create

interface XmlCalabashBuildExtension {
  abstract val project: Property<Project>
  abstract val group: Property<String>
  abstract val name: Property<String>
  abstract val version: Property<String>
  abstract val productName: Property<String>
  abstract val vendorName: Property<String>
  abstract val vendorUri: Property<String>
  abstract val buildTime: Property<String>
  abstract val buildDate: Property<String>

  fun jarArchiveFilename(): String {
    return "${name.get()}-${version.get()}.jar"
  }

  fun gitHash(): String {
    val commandLine = listOf("git", "rev-parse", "--short", "HEAD")
    val builder = ProcessBuilder(commandLine)
    val process = builder.start()

    val stdout = ByteArrayOutputStream()
    val stderr = ByteArrayOutputStream()

    val stdoutReader = ProcessOutputReader(process.inputStream, stdout)
    val stderrReader = ProcessOutputReader(process.errorStream, stderr)

    val stdoutThread = Thread(stdoutReader)
    val stderrThread = Thread(stderrReader)

    stdoutThread.start()
    stderrThread.start()

    val rc = process.waitFor()
    stdoutThread.join()
    stderrThread.join()

    if (rc != 0) {
      throw RuntimeException("Attempt to get git hash failed: ${rc}")
    }

    return stdout.toString().trim()
  }
}

internal class ProcessOutputReader(val stream: InputStream,
                                   val buffer: ByteArrayOutputStream): Runnable {
  override fun run() {
    val reader = InputStreamReader(stream)
    val buf = CharArray(4096)
    var len = reader.read(buf)
    while (len >= 0) {
      if (len == 0) {
        Thread.sleep(250)
      } else {
        // This is the most efficient way? Really!?
        for (pos in 0 until len) {
          buffer.write(buf[pos].code)
        }
      }
      len = reader.read(buf)
    }
  }
}

class XmlCalabashBuildPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val buildTime = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS);
    val extension = project.extensions.create<XmlCalabashBuildExtension>("xmlcalabash")
    extension.project.set(project)
    extension.group.set(project.findProperty("xmlcalabashGroup").toString())
    extension.name.set(project.name)
    extension.version.set(project.findProperty("xmlcalabashVersion").toString())
    extension.productName.set("XML Calabash")
    extension.vendorName.set("Norm Tovey-Walsh")
    extension.vendorUri.set("https://xmlcalabash.com/")
    extension.buildTime.set(DateTimeFormatter.ISO_INSTANT.format(buildTime))
    extension.buildDate.set(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(buildTime))
  }
}
