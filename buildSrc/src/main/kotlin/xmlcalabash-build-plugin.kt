package com.xmlcalabash.build

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import org.gradle.api.artifacts.Configuration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.MapProperty
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
  abstract val buildDateId: Property<String>

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

  fun buildId(): String {
    return "${gitHash()}.${buildDateId.get()}"
  }

  fun unversionedName(name: String): String {
    val vregex = "^(.*)-(\\d+)(\\.\\d+)?(\\.\\d+)?(-.*)?\\.jar$".toRegex()
    val result = vregex.matchEntire(name)
    if (result != null) {
      val prefix = result.groups[1]!!.value
      val suffix = result.groups[5]?.value ?: ""
      return "${prefix}${suffix}.jar"
    }
    return name
  }

  fun dependencyMap(config: Configuration): Map<String,File> {
    val versions = mutableMapOf<String,File>()
    config.forEach {
      val pathname = it.toString().replace("\\", "/")
      val pos = pathname.lastIndexOf("/")
      val filename = pathname.substring(pos+1)
      versions[unversionedName(filename)] = it
    }

    return versions
  }

  fun distClasspath(mainConfig: Configuration, stepConfig: Configuration): List<File> {
    val xmlcalabashMap = dependencyMap(mainConfig)
    val stepMap = dependencyMap(stepConfig)

    var message = false

    val libs = mutableListOf<File>()
    for ((name, jar) in stepMap) {
      if (name !in xmlcalabashMap) {
        libs.add(jar)
      } else {
        if (jar != xmlcalabashMap[name]) {
          if (!message) {
            println("Skipping local dependencies in favor of version in XML Calabash")
            message = true
          }
          println("  Skip ${jar.name} for ${xmlcalabashMap[name]?.name}")
        }
      }
    }
    return libs
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
    extension.buildDateId.set(String.format("%02x%x.%02x%02x%02x",
                                            buildTime.year % 100,
                                            buildTime.monthValue,
                                            buildTime.dayOfMonth, buildTime.hour, buildTime.minute))
  }
}
