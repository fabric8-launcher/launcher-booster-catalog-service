/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi

import java.nio.file.Path

import io.fabric8.launcher.booster.catalog.Booster
import io.fabric8.launcher.booster.catalog.utils.cloneRepository
import java.nio.file.Files

/**
 * Default implementation for [BoosterCatalogSourceProvider]
 */
open class NativeGitCatalogSourceProvider(private val rootDir: Path? = null) {
    private var tempDir: Path? = null

    open val fetchSource: BoosterCatalogSourceProvider = { booster ->
        val gitRepo = booster.gitRepo
        val gitRef = booster.gitRef
        val path = createContentPath(booster)
        assert(gitRepo != null)
        assert(gitRef != null)
        cloneRepository(gitRepo!!, gitRef!!, path)
    }

    private fun createContentPath(booster: Booster): Path {
        val path = targetDir.resolve(booster.id)
        Files.createDirectories(path)
        return path
    }

    internal val targetDir: Path
        get() {
            var path = rootDir ?: tempDir
            if (path == null) {
                path = Files.createTempDirectory("booster-catalog")
                tempDir = path
                return path
            } else {
                return path
            }
        }
}
