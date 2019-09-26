/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster

import io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBoosterCatalogService
import io.fabric8.launcher.booster.catalog.spi.NativeGitCatalogSourceProvider
import io.fabric8.launcher.booster.catalog.utils.readCatalog
import io.fabric8.launcher.booster.catalog.utils.readMetadata
import org.yaml.snakeyaml.Yaml

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

/**
 * Indexes a Booster catalog and logs any problems it finds to the console
 *
 * @author [Tako Schotanus](mailto:tschotan@redhat.com)
 */
internal object BoosterValidator {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val catalog = if (args.size > 0) args[0] else "catalog.json"
        val metadata = if (args.size > 1) args[1] else "metadata.json"

        // Silence all INFO logging
        val handlers = Logger.getLogger("").handlers
        for (index in handlers.indices) {
            handlers[index].level = Level.WARNING
        }

        val build = RhoarBoosterCatalogService.Builder()
                .catalogProvider({ readCatalog(Paths.get(catalog)) })
                .metadataProvider({ readMetadata(Paths.get(metadata)) })
                .sourceProvider(NativeGitCatalogSourceProvider().fetchSource)
                .build()

        println("Validating Booster Catalog $catalog")
        println("Fetching index...")
        val boosters = build.index().get()
        println("Done.")

        println("Fetching boosters...")
        val errcnt = AtomicInteger(0)
        val futures = ArrayList<CompletableFuture<Path>>()
        for (b in boosters) {
            futures.add(b.content().whenComplete { path, throwable ->
                if (throwable != null) {
                    System.err.println("ERROR: Couldn't fetch Booster " + b.id)
                    errcnt.incrementAndGet()
                } else {
                    println("Fetched " + b.id + " (" + b.name + ")")
                    if (!validateBoosterData(b, path, b.data)) {
                        errcnt.incrementAndGet()
                    }
                    if (!validateOpenshiftYamlFiles(b, path)) {
                        errcnt.incrementAndGet()
                    }
                    if (!validateBooster(b, path)) {
                        errcnt.incrementAndGet()
                    }
                    System.out.flush()
                    System.err.flush()
                }
            })
        }

        try {
            CompletableFuture.allOf(*futures.toTypedArray()).join()
        } catch (e: CompletionException) {
        }

        if (errcnt.get() == 0) {
            println("Done. No problems found.")
        } else {
            println("Done. " + errcnt.get() + " errors were encountered.")
            exitProcess(1)
        }
    }

    private fun validateOpenshiftYamlFiles(booster: RhoarBooster, path: Path): Boolean {
        val yaml = Yaml()
        val valid = AtomicBoolean(true)
        try {
            Files.walk(path)
                    .filter { p -> p.parent.fileName.toString() == ".openshiftio" }
                    .filter { p -> p.fileName.toString().endsWith(".yaml") || p.fileName.toString().endsWith(".yml") }
                    .forEach { p ->
                        println("    Validating " + booster.id + " - " + path.relativize(p))
                        try {
                            Files.newBufferedReader(p).use { reader -> yaml.loadAs(reader, Map::class.java) }
                        } catch (e: Exception) {
                            System.err.println("    ERROR: Parse error in " + booster.id + " - " + path.relativize(p) + ": " + e.message)
                            valid.set(false)
                        }
                    }
        } catch (e: IOException) {
            System.err.println("    ERROR: IO error: " + booster.id + " - " + e.message)
            valid.set(false)
        }

        return valid.get()
    }

    private fun validateBoosterData(booster: RhoarBooster, path: Path, data: Map<*, *>): Boolean {
        var valid = true
        val entries = data.entries
        for ((key1, value) in entries) {
            val key = key1.toString()
            if (key.contains(" ")) {
                System.err.println("    ERROR: Parse error in " + booster.id + " - " + path.relativize(path) + ": Keys should not contain spaces: '" + key + "'")
                valid = false
            }
            if (value is Map<*, *>) {
                valid = valid && validateBoosterData(booster, path, value)
            } else if (value is Iterable<*>) {
                valid = valid && validateBoosterData(booster, path, value)
            }
        }
        return valid
    }

    private fun validateBoosterData(booster: RhoarBooster, path: Path, data: Iterable<*>): Boolean {
        var valid = true
        for (value in data) {
            if (value is Map<*, *>) {
                valid = valid && validateBoosterData(booster, path, value)
            } else if (value is Iterable<*>) {
                valid = valid && validateBoosterData(booster, path, value)
            }
        }
        return valid
    }

    private fun validateBooster(booster: RhoarBooster, path: Path): Boolean {
        var valid = true
        if (booster.runtime?.name == booster.runtime?.id && booster.runtime?.description == null) {
            System.err.println("    ERROR: Content error in ${booster.id} - ${path.relativize(path)}: Runtime '${booster.runtime?.id}' not found in metadata")
            valid = false
        }
        if (booster.version?.name == booster.version?.id && booster.version?.description == null) {
            System.err.println("    ERROR: Content error in ${booster.id} - ${path.relativize(path)}: Version '${booster.version?.id}' not found in metadata")
            valid = false
        }
        if (booster.mission?.name == booster.mission?.id && booster.mission?.description == null) {
            System.err.println("    ERROR: Content error in ${booster.id} - ${path.relativize(path)}: Mission '${booster.mission?.id}' not found in metadata")
            valid = false
        }
        return valid
    }
}
