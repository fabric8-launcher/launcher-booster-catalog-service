/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi

import io.fabric8.launcher.booster.catalog.Booster

import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger

class ZipBoosterCatalogPathProvider(private val catalogZipURL: URL) : BoosterCatalogPathProvider {

    @Throws(IOException::class)
    override fun createCatalogPath(): Path {
        val catalogPath = Files.createTempDirectory("booster-catalog")
        logger.info("Unzipping booster contents to $catalogPath")
        val catalogPathZip = catalogPath.resolve("booster.zip")
        // Copy to temp folder
        catalogZipURL.openStream().use { `is` -> Files.copy(`is`, catalogPathZip) }
        io.fabric8.launcher.booster.Files.unzip(catalogPathZip, catalogPath)
        Files.delete(catalogPathZip)
        return catalogPath
    }

    override fun createBoosterContentPath(booster: Booster): Path {
        throw IllegalStateException("Could not find the content path for $booster")
    }

    companion object {
        private val logger = Logger.getLogger(ZipBoosterCatalogPathProvider::class.java.name)
    }
}
