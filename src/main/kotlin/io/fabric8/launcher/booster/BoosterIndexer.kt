/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster

import io.fabric8.launcher.booster.catalog.AbstractBoosterCatalogService
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import io.fabric8.launcher.booster.catalog.BoosterCatalogService
import io.fabric8.launcher.booster.catalog.spi.NativeGitBoosterCatalogPathProvider
import java.util.function.Predicate

/**
 * Indexes a Booster catalog and adds all its contents to a ZIP file
 *
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 */
internal object BoosterIndexer {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val catalogRepository = args[0]
        val catalogRef = args[1]
        val targetDir = Paths.get(args[2])
        val targetZip = Paths.get(args[3])

        val build = BoosterCatalogService.Builder()
                .pathProvider(NativeGitBoosterCatalogPathProvider(catalogRepository, catalogRef, targetDir))
                .build()

        build.index().get()
        build.prefetchBoosters().get()
        Files.newOutputStream(targetZip).use { os ->
            io.fabric8.launcher.booster.Files.zip("", targetDir, os,
                    Predicate { path -> !AbstractBoosterCatalogService.EXCLUDED_PROJECT_FILES.contains(path.toFile().getName().toLowerCase()) })
        }
    }

}
