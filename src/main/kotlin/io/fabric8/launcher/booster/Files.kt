/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.Predicate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 */
object Files {
    fun removeFileExtension(file: String): String {
        val idx = file.lastIndexOf('.')
        return if (idx > 0) file.substring(0, idx) else file
    }

    /**
     * Zips an entire directory and stores in the provided [OutputStream]
     *
     * @param root      the root directory to be used
     * @param directory the directory to be zipped
     * @param os        the [OutputStream] which the zip operation will be written to
     * @throws IOException if any I/O error happens
     */
    @Throws(IOException::class)
    fun zip(root: String, directory: Path, os: OutputStream, filter: Predicate<Path>) {
        ZipOutputStream(os).use { zos ->
            java.nio.file.Files.walkFileTree(directory, object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!filter.test(file)) {
                        return FileVisitResult.CONTINUE
                    }
                    val entry = root + File.separator + directory.relativize(file).toString()
                    zos.putNextEntry(ZipEntry(entry))
                    java.nio.file.Files.copy(file, zos)
                    zos.closeEntry()
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!filter.test(dir)) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    val entry = root + File.separator + directory.relativize(dir).toString() + File.separator
                    zos.putNextEntry(ZipEntry(entry))
                    zos.closeEntry()
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }

    /**
     * Unzips a ZIP file in the target directory, preserving the directory structure
     *
     * @param zipFile
     * @param targetDir
     * @throws IOException
     */
    @Throws(IOException::class)
    fun unzip(zipFile: Path, targetDir: Path) {
        ZipFile(zipFile.toFile()).use { zf ->
            zf.stream().forEach( { ze ->
                val newEntry = targetDir.resolve(ze.name).normalize()
                if (!newEntry.startsWith(targetDir)) {
                    throw IOException("Illegal Zip entry name: ${ze.name}")
                }
                if (ze.isDirectory) {
                    java.nio.file.Files.createDirectories(newEntry)
                } else {
                    zf.getInputStream(ze).use { zis ->
                        java.nio.file.Files.copy(zis, newEntry)
                    }
                }
            })
        }
    }

    /**
     * @throws IOException
     */
    fun deleteRecursively(path: Path) {
        try {
            if (java.nio.file.Files.isDirectory(path)) {
                java.nio.file.Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
                    @Throws(IOException::class)
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        java.nio.file.Files.delete(file)
                        return FileVisitResult.CONTINUE
                    }

                    @Throws(IOException::class)
                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        java.nio.file.Files.delete(dir)
                        return FileVisitResult.CONTINUE
                    }
                })
            } else {
                java.nio.file.Files.deleteIfExists(path)
            }
        } catch (ignored: IOException) {
        }

    }

}
