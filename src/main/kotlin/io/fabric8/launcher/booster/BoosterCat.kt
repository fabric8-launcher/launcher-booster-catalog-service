/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster

import io.fabric8.launcher.booster.catalog.BoosterDataTransformer
import io.fabric8.launcher.booster.catalog.LauncherConfiguration
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBoosterCatalogService
import io.fabric8.launcher.booster.catalog.spi.NativeGitBoosterCatalogPathProvider
import org.yaml.snakeyaml.Yaml

import java.io.OutputStreamWriter
import java.util.ArrayList
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Indexes a Booster catalog and logs any problems it finds to the console
 *
 * @author [Tako Schotanus](mailto:tschotan@redhat.com)
 */
internal object BoosterCat {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val catalogRepository = if (args.size > 0 && !args[0].isEmpty()) args[0] else LauncherConfiguration.boosterCatalogRepositoryURI()
        val catalogRef = if (args.size > 1 && !args[1].isEmpty()) args[1] else LauncherConfiguration.boosterCatalogRepositoryRef()
        val env = if (args.size > 2 && !args[2].isEmpty()) args[2] else "development"

        // Silence all INFO logging
        val handlers = Logger.getLogger("").handlers
        for (index in handlers.indices) {
            handlers[index].level = Level.WARNING
        }

        val build = RhoarBoosterCatalogService.Builder()
                .pathProvider(NativeGitBoosterCatalogPathProvider(catalogRepository, catalogRef, null))
                .build()

        val boosters = build.index().get()

        val list = ArrayList<Map<String, Any?>>(boosters.size)
        for (metab in boosters) {
            val b = metab.forEnvironment(env)
            val data = HashMap(b.exportableData)
            if (!env.isEmpty()) {
                data.remove("environment")
            }
            list.add(data)
        }

        val yaml = Yaml()
        yaml.dump(list, OutputStreamWriter(System.out))
    }
}
