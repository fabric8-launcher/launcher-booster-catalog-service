/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.Predicate

/**
 * A [SimpleFileVisitor] implementation used to copy files from a directory [Path] to another directory.
 *
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 */
class CopyFileVisitor(private val targetPath: Path, private val filter: Predicate<Path>) : SimpleFileVisitor<Path>() {

    private var sourcePath: Path? = null

    @Throws(IOException::class)
    override fun preVisitDirectory(dir: Path,
                                   attrs: BasicFileAttributes): FileVisitResult {
        if (!filter.test(dir)) {
            return FileVisitResult.SKIP_SUBTREE
        }
        if (sourcePath == null) {
            sourcePath = dir
        } else {
            val target = targetPath.resolve(sourcePath!!.relativize(dir))
            java.nio.file.Files.createDirectories(target)
        }
        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun visitFile(file: Path,
                           attrs: BasicFileAttributes): FileVisitResult {
        if (sourcePath != null && filter.test(file)) {
            val target = targetPath.resolve(sourcePath!!.relativize(file))
            java.nio.file.Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
        }
        return FileVisitResult.CONTINUE
    }
}
