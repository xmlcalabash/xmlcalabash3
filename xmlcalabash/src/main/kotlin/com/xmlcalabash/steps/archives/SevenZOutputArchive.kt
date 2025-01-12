package com.xmlcalabash.steps.archives

import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.kotlin.logger
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry

class SevenZOutputArchive(stepConfig: XProcStepConfiguration): OutputArchive(stepConfig) {
    override val archiveFormat = Ns.sevenZ
    lateinit var sevenZArchive: SevenZOutputFile

    override fun create(file: File?) {
        if (file == null) {
            archiveFile = createArchive(".zip")
        } else {
            archiveFile = file.toPath()
        }

        sevenZArchive = SevenZOutputFile(file)
    }

    override fun close() {
        sevenZArchive.close()
    }

    override fun write(entry: XArchiveEntry) {
        val szEntry = SevenZArchiveEntry()
        szEntry.name = entry.name
        sevenZArchive.putArchiveEntry(szEntry)

        fileTime(entry.properties[NsCx.fileCreationTime])?.let { szEntry.creationTime = it }
        fileTime(entry.properties[NsCx.lastAccessTime])?.let { szEntry.accessTime = it }
        fileTime(entry.properties[NsCx.lastModified])?.let { szEntry.lastModifiedTime = it }
        // setWindowsAttributes
        if (entry.properties[NsCx.antiItem] == "true") {
            szEntry.isAntiItem = true
        }

        if (entry.method != null) {
            val methods = mutableListOf<SevenZMethodConfiguration>()
            val tokens = entry.method!!.split("\\s+".toRegex())
            for (token in tokens) {
                when (token) {
                    "none" -> methods.add(SevenZMethodConfiguration(SevenZMethod.COPY))
                    "lzma" -> methods.add(SevenZMethodConfiguration(SevenZMethod.LZMA))
                    "lzma2" -> methods.add(SevenZMethodConfiguration(SevenZMethod.LZMA2))
                    "deflate" -> methods.add(SevenZMethodConfiguration(SevenZMethod.DEFLATE))
                    "deflate64" -> methods.add(SevenZMethodConfiguration(SevenZMethod.DEFLATE64))
                    "bzip2" -> methods.add(SevenZMethodConfiguration(SevenZMethod.BZIP2))
                    else -> stepConfig.debug { "Ignoring unknown 7z compression method: ${token}" }
                }
            }
            if (methods.isNotEmpty()) {
                szEntry.contentMethods = methods
            }
        }

        if (entry.document == null) {
            sevenZArchive.write(entry.inputStream!!)
        } else {
            val baos = ByteArrayOutputStream()
            entry.write(baos)
            sevenZArchive.write(baos.toByteArray())
        }

        sevenZArchive.closeArchiveEntry()

        super.write(entry)
    }
}