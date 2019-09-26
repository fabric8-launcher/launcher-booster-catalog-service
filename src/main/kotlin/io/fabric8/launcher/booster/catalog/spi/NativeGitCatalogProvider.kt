/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi

import java.nio.file.Path
import java.util.logging.Logger

import io.fabric8.launcher.booster.catalog.utils.cloneRepository
import io.fabric8.launcher.booster.catalog.utils.readCatalog
import io.fabric8.launcher.booster.catalog.utils.readMetadata

/**
 * Default implementation for [BoosterCatalogProvider]
 * @param catalogRepositoryURI
 * @param catalogRef
 */
open class NativeGitCatalogProvider(private val catalogRepositoryURI: String, private val catalogRef: String, private val rootDir: Path? = null): NativeGitCatalogSourceProvider(rootDir) {

    val fetchCatalog: BoosterCatalogProvider = {
        readCatalog(catalogPath.resolve("catalog.json"))
    }

    val fetchMetadata: BoosterMetadataProvider = {
        readMetadata(catalogPath.resolve("metadata.json"))
    }

    private val catalogPath: Path by lazy {
        cloneRepository(catalogRepositoryURI, catalogRef, targetDir)
    }

    companion object {
        private val logger = Logger.getLogger(NativeGitCatalogProvider::class.java.name)
    }

}
