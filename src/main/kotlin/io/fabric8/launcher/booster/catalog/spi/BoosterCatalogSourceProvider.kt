/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi

import java.io.IOException
import java.nio.file.Path

import io.fabric8.launcher.booster.catalog.Booster
import io.fabric8.launcher.booster.catalog.BoosterCatalogService

/**
 * A SPI interface used in [BoosterCatalogService]
 */
interface BoosterCatalogSourceProviderx {
    /**
     * Fetches the content for a given booster and returns the target [Path]
     */
    @Throws(IOException::class)
    fun fetchSource(booster: Booster): Path
}

typealias BoosterCatalogSourceProvider = (Booster) -> Path