/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.fabric8.launcher.booster.catalog.Booster;

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
            logger.log(Level.INFO, "Created {0}", catalogPath);
        }
        return cloneRepository(catalogRepositoryURI, catalogRef, catalogPath);
    }

    @Override
    public Path createBoosterContentPath(Booster booster) throws IOException {
        String gitRepo = booster.getGitRepo();
        String gitRef = booster.getGitRef();
        Path targetPath = booster.getContentPath();
        Objects.requireNonNull(gitRepo, "Booster.getGitRepo should not be null");
        Objects.requireNonNull(gitRef, "Booster.getGitRef should not be null");
        Objects.requireNonNull(targetPath, "Booster.getContentPath should not be null");
        return cloneRepository(gitRepo, gitRef, targetPath);
    }

    private Path cloneRepository(String repo, String ref, Path targetPath) throws IOException {
        assert (targetPath != null);
        int exitCode;
        try {
            ProcessBuilder builder = new ProcessBuilder()
                    .command("git", "clone", repo,
                             "--branch", ref,
                             "--recursive",
                             "--depth=1",
                             "--quiet",
                             "-c", "advice.detachedHead=false",
                             targetPath.toString())
                    .inheritIO();
            logger.info(() -> "Executing: " + builder.command().stream().collect(Collectors.joining(" ")));
            exitCode = builder.start().waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Process returned exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            // Restore interrupted state
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted cloning process");
            throw new IOException("Interrupted", e);
        }
        return targetPath;
    }

}