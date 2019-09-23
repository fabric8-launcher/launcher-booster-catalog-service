/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster

import io.fabric8.launcher.booster.catalog.LauncherConfiguration
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBoosterCatalogService
import io.fabric8.launcher.booster.catalog.spi.NativeGitBoosterCatalogPathProvider
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.io.OutputStreamWriter
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Dumps the entire contents of the catalog to stdout
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
        Logger.getLogger("").handlers.forEach { it.level = Level.WARNING }

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

        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.isPrettyFlow = true
        val yaml = Yaml(options)
        yaml.dump(list, OutputStreamWriter(System.out))
    }
}

/**
 * Dumps the entire contents of the catalog to stdout using a simplified structure
 *
 * @author [Tako Schotanus](mailto:tschotan@redhat.com)
 */
internal object BoosterSingleDoc {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val catalogRepository = if (args.size > 0 && !args[0].isEmpty()) args[0] else LauncherConfiguration.boosterCatalogRepositoryURI()
        val catalogRef = if (args.size > 1 && !args[1].isEmpty()) args[1] else LauncherConfiguration.boosterCatalogRepositoryRef()
        val env = if (args.size > 2 && !args[2].isEmpty()) args[2] else "production"

        // Silence all INFO logging
        Logger.getLogger("").handlers.forEach { it.level = Level.WARNING }

        val build = RhoarBoosterCatalogService.Builder()
                .pathProvider(NativeGitBoosterCatalogPathProvider(catalogRepository, catalogRef, null))
                .build()

        val boosters = build.index().get()

        val result = ArrayList<Map<String, Any?>>(boosters.size)
        for (metab in boosters) {
            val b = metab.forEnvironment(env)
            val data = LinkedHashMap<String, Any?>()
//            data["id"] = "" + strip(b.exportableData["runtime"]) + "-" + strip(b.exportableData["version"]) + "-" + strip(b.exportableData["mission"])
            data["name"] = b.exportableData["name"]
            data["description"] = b.exportableData["description"]
            (b.exportableData["source"] as? HashMap<String, Any?>)?.let {
                (it["git"] as? HashMap<String, Any?>)?.let {
//                    val url = it["url"]
//                    val ref = it["ref"] ?: "master"
//                    if (url != null) {
//                        data["url"] = "$url/archive/$ref.zip"
//                    }
                    data["repo"] = it["url"]
                    it["ref"]?.let { data["ref"] = it }
                }
            }
            val md = LinkedHashMap<String, Any?>()
            md["mission"] = b.exportableData["mission"]
            md["runtime"] = b.exportableData["runtime"]
            md["version"] = b.exportableData["version"]
            data["metadata"] = md
            result.add(data)
        }

        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.FLOW
        options.isPrettyFlow = true
        options.defaultScalarStyle = DumperOptions.ScalarStyle.DOUBLE_QUOTED
        options.splitLines = false
        val yaml = Yaml(options)
        yaml.dump(result, OutputStreamWriter(System.out))
    }

    private fun strip(value: Any?): String = if (value is String) {
        value.replace("""[^\p{Alnum}]""".toRegex(), "")
    } else ""
}
