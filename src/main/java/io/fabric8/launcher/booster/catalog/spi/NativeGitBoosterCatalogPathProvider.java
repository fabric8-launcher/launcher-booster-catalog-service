/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi;

import io.fabric8.launcher.booster.catalog.Booster;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Default implementation for {@link BoosterCatalogPathProvider}
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NativeGitBoosterCatalogPathProvider implements BoosterCatalogPathProvider {
    /**
     * @param catalogRepositoryURI
     * @param catalogRef
     */
    public NativeGitBoosterCatalogPathProvider(String catalogRepositoryURI, String catalogRef, @Nullable Path rootDir) {
        super();
        this.catalogRepositoryURI = catalogRepositoryURI;
        this.catalogRef = catalogRef;
        this.rootDir = rootDir;
    }

    private static final Logger logger = Logger.getLogger(NativeGitBoosterCatalogPathProvider.class.getName());

    private final String catalogRepositoryURI;

    private final String catalogRef;

    @Nullable
    private final Path rootDir;

    @Override
    public Path createCatalogPath() throws IOException {
        logger.log(Level.INFO, "Indexing contents from {0} using {1} ref",
                   new Object[]{catalogRepositoryURI, catalogRef});
        Path catalogPath = rootDir;
        if (catalogPath == null) {
            catalogPath = Files.createTempDirectory("booster-catalog");
            logger.info("Created " + catalogPath);
        }
        ProcessBuilder builder = new ProcessBuilder()
                .command("git", "clone", catalogRepositoryURI,
                        "--branch", catalogRef,
                        "--recursive",
                        "--depth=1",
                        "--quiet",
                        catalogPath.toString())
                .inheritIO();
        logger.info("Executing: " + builder.command().stream().collect(Collectors.joining(" ")));
        try {
            int exitCode = builder.start().waitFor();
            assert exitCode == 0 : "Process returned exit code: " + exitCode;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted indexing process");
            throw new IOException("Interrupted", e);
        }
        return catalogPath;
    }

    @Override
    public Path createBoosterContentPath(Booster booster) throws IOException {
        int exitCode;
        try {
            Path contentPath = booster.getContentPath();
            assert(contentPath != null);
            ProcessBuilder builder = new ProcessBuilder()
                    .command("git", "clone", booster.getGitRepo(),
                             "--branch", booster.getGitRef(),
                             "--recursive",
                             "--depth=1",
                             "--quiet",
                             contentPath.toString())
                    .inheritIO();
            logger.info("Executing: " + builder.command().stream().collect(Collectors.joining(" ")));
            exitCode = builder.start().waitFor();
            assert exitCode == 0 : "Process returned exit code: " + exitCode;
            builder = new ProcessBuilder()
                    .command("git", "checkout", booster.getGitRef(), "--quiet")
                    .directory(contentPath.toFile())
                    .inheritIO();
            logger.info("Executing: " + builder.command().stream().collect(Collectors.joining(" ")));
            exitCode = builder.start().waitFor();
            assert exitCode == 0 : "Process returned exit code: " + exitCode;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted booster fetching process");
            throw new IOException("Interrupted", e);
        }
        return booster.getContentPath();
    }

}