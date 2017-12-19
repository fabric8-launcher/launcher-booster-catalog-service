/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster;

import io.openshift.booster.catalog.BoosterCatalogService;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Files {
    public static String removeFileExtension(String file) {
        int idx = file.lastIndexOf('.');
        return idx > 0 ? file.substring(0, idx) : file;
    }

    /**
     * Zips an entire directory and stores in the provided {@link OutputStream}
     *
     * @param root      the root directory to be used
     * @param directory the directory to be zipped
     * @param os        the {@link OutputStream} which the zip operation will be written to
     * @throws IOException if any I/O error happens
     */
    public static void zip(String root, final Path directory, OutputStream os) throws IOException {
        try (final ZipOutputStream zos = new ZipOutputStream(os)) {
            java.nio.file.Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (BoosterCatalogService.EXCLUDED_PROJECT_FILES.contains(file.toFile().getName())) {
                        return FileVisitResult.CONTINUE;
                    }
                    String entry = root + File.separator + directory.relativize(file).toString();
                    zos.putNextEntry(new ZipEntry(entry));
                    java.nio.file.Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (BoosterCatalogService.EXCLUDED_PROJECT_FILES.contains(dir.toFile().getName())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    String entry = root + File.separator + directory.relativize(dir).toString() + File.separator;
                    zos.putNextEntry(new ZipEntry(entry));
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Unzips a ZIP file in the target directory, preserving the directory structure
     *
     * @param zipFile
     * @param targetDir
     * @throws IOException
     */
    public static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(java.nio.file.Files.newInputStream(zipFile))) {
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                Path newEntry = Paths.get(targetDir.toString(), ze.getName());
                if (ze.isDirectory()) {
                    java.nio.file.Files.createDirectories(newEntry);
                } else {
                    try (OutputStream os = java.nio.file.Files.newOutputStream(newEntry)) {
                        byte[] buffer = new byte[512];
                        int size;
                        while ((size = zin.read(buffer, 0, buffer.length)) != -1) {
                            os.write(buffer, 0, size);
                        }
                    }
                }
                zin.closeEntry();
            }
        }
    }

    /**
     * @throws IOException
     */
    public static void deleteRecursively(Path path) throws IOException {
        if (java.nio.file.Files.isDirectory(path)) {
            java.nio.file.Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    java.nio.file.Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    java.nio.file.Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            java.nio.file.Files.deleteIfExists(path);
        }
    }

}
