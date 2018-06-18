/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

import io.fabric8.launcher.booster.catalog.Booster

/**
 * Default implementation for [BoosterCatalogPathProvider]
 * @param catalogRepositoryURI
 * @param catalogRef
 */
open class NativeGitBoosterCatalogPathProvider(private val catalogRepositoryURI: String, private val catalogRef: String, private val rootDir: Path?) : BoosterCatalogPathProvider {

    @Throws(IOException::class)
    override fun createCatalogPath(): Path {
        logger.log(Level.INFO, "Indexing contents from {0} using {1} ref",
                arrayOf<Any>(catalogRepositoryURI, catalogRef))
        var catalogPath = rootDir
        if (catalogPath == null) {
            catalogPath = Files.createTempDirectory("booster-catalog")
            logger.log(Level.INFO, "Created {0}", catalogPath)
        }
        return cloneRepository(catalogRepositoryURI, catalogRef, catalogPath!!)
    }

    @Throws(IOException::class)
    override fun createBoosterContentPath(booster: Booster): Path {
        val gitRepo = booster.gitRepo
        val gitRef = booster.gitRef
        val targetPath = booster.contentPath
        assert(gitRepo != null)
        assert(gitRef != null)
        assert(targetPath != null)
        return cloneRepository(gitRepo!!, gitRef!!, targetPath!!)
    }

    @Throws(IOException::class)
    private fun cloneRepository(repo: String, ref: String, targetPath: Path): Path {
        val exitCode: Int
        try {
            val builder = ProcessBuilder()
                    .command("git", "clone", repo,
                            "--branch", ref,
                            "--recursive",
                            "--depth=1",
                            "--quiet",
                            "-c", "advice.detachedHead=false",
                            targetPath.toString())
                    .inheritIO()
            logger.info { "Executing: " + builder.command().stream().collect(Collectors.joining(" ")) }
            exitCode = builder.start().waitFor()
            if (exitCode != 0) {
                throw IllegalStateException("Process returned exit code: $exitCode")
            }
        } catch (e: InterruptedException) {
            // Restore interrupted state
            Thread.currentThread().interrupt()
            logger.log(Level.WARNING, "Interrupted cloning process")
            throw IOException("Interrupted", e)
        }

        return targetPath
    }

    companion object {

        private val logger = Logger.getLogger(NativeGitBoosterCatalogPathProvider::class.java.name)
    }

}