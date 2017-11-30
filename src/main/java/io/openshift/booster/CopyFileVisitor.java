/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

/**
 * A {@link SimpleFileVisitor} implementation used to copy files from a directory {@link Path} to another directory.
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class CopyFileVisitor extends SimpleFileVisitor<Path> {
    public CopyFileVisitor(Path targetPath, Predicate<Path> filter) {
        this.targetPath = targetPath;
        this.filter = filter;
    }

    private final Path targetPath;

    private final Predicate<Path> filter;

    private Path sourcePath;

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
                                             final BasicFileAttributes attrs) throws IOException {
        if (!filter.test(dir)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        if (sourcePath == null) {
            sourcePath = dir;
        } else {
            Path target = targetPath.resolve(sourcePath.relativize(dir));
            java.nio.file.Files.createDirectories(target);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file,
                                     final BasicFileAttributes attrs) throws IOException {
        if (filter.test(file)) {
            Path target = targetPath.resolve(sourcePath.relativize(file));
            java.nio.file.Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return FileVisitResult.CONTINUE;
    }
}
