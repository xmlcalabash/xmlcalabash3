package com.xmlcalabash.steps.file

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import net.sf.saxon.s9api.QName
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class FileCopyOrMove(stepType: QName): FileStep(stepType) {
    protected fun copyOrMove() {
        val href = try {
            uriBinding(Ns.href)!!
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdInvalidUri(options[Ns.href].toString()), ex)
        }

        if (href.scheme != "file") {
            if (stepType == NsP.fileCopy) {
                throw stepConfig.exception(XProcError.xcUnsupportedFileCopyScheme(href.scheme))
            }
            throw stepConfig.exception(XProcError.xcUnsupportedFileMoveScheme(href.scheme))
        }

        val targetHref = try {
            uriBinding(Ns.target)!!
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdInvalidUri(options[Ns.target].toString()), ex)
        }

        if (targetHref.scheme != "file") {
            if (stepType == NsP.fileCopy) {
                throw stepConfig.exception(XProcError.xcUnsupportedFileCopyScheme(href.scheme))
            }
            throw stepConfig.exception(XProcError.xcUnsupportedFileMoveScheme(href.scheme))
        }

        failOnError = booleanBinding(Ns.failOnError) ?: true

        if (options.containsKey(Ns.overwrite)) {
            overwrite = booleanBinding(Ns.overwrite) ?: true
        }

        val source = File(href.path)
        if (!source.exists()) {
            maybeThrow(XProcError.xdDoesNotExist(href.path, "path does not exist"), href)
            return
        }

        var target = File(targetHref.path)
        if (source.isDirectory) {
            if (target.exists() && target.isFile) {
                if (stepType == NsP.fileCopy) {
                    maybeThrow(XProcError.xcCopyDirectoryToFile(source.toURI(), target.toURI()), source.toURI(), target.toURI())
                } else {
                    maybeThrow(XProcError.xcMoveDirectoryToFile(source.toURI(), target.toURI()), source.toURI(), target.toURI())
                }
                return
            }
        }

        if (target.exists()) {
            if (target.isDirectory) {
                target = File(target, source.name)
            }
        } else {
            if (stepType == NsP.fileCopy && source.isDirectory) {
                target = File(target, source.name)
            }
        }

        if (target.exists() && !overwrite && stepType == NsP.fileMove) {
            maybeThrow(XProcError.xcAttemptToOverwrite(target.absolutePath), source.toURI(), target.toURI())
            return
        }

        if (!target.exists() || !target.isFile || overwrite) {
            if (source.isFile) {
                if (!copyFile(source, target)) {
                    return
                }
            } else {
                if (!copyDirectory(source, target)) {
                    return
                }
            }

            if (source.isDirectory) {
                source.deleteRecursively()
            }
        }

        val result = resultDocument(targetHref)
        receiver.output("result", XProcDocument.ofXml(result, stepConfig))
    }

    private fun copyFile(source: File, target: File): Boolean {
        val targetPath = target.toPath()
        val sourcePath = source.toPath()

        if (target.exists() && !overwrite) {
            return true
        }

        try {
            val parent = File(target.parent)
            if (parent.exists() && parent.isFile) {
                parent.delete()
            }
            parent.mkdirs()
            target.deleteRecursively()
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
        } catch (ex: IOException) {
            maybeThrow(XProcError.xcCannotCopy(source.toURI(), target.toURI()), source.toURI(), target.toURI())
            return false
        }

        if (stepType == NsP.fileMove) {
            return source.delete()
        }
        return true
    }

    private fun copyDirectory(source: File, target: File): Boolean {
        for (sourceFile in source.listFiles()!!) {
            val targetFile = File(target, sourceFile.name)
            val ok = if (sourceFile.isDirectory) {
                copyDirectory(sourceFile, targetFile)
            } else {
                copyFile(sourceFile, targetFile)
            }
            if (!ok) {
                return false
            }
        }
        return true
    }

}