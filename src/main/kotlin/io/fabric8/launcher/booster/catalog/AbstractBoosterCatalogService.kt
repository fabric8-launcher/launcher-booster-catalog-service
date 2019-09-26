/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Arrays
import java.util.Collections
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream

import io.fabric8.launcher.booster.CopyFileVisitor
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogProvider
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogSourceProvider
import io.fabric8.launcher.booster.catalog.spi.NativeGitCatalogProvider

/**
 * This service reads from the Booster catalog Github repository in https://github.com/openshiftio/booster-catalog and
 * marshalls into [Booster] objects.
 *
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 * @author [Tako Schotanus](mailto:tschotan@redhat.com)
 */
abstract class AbstractBoosterCatalogService<BOOSTER : Booster> protected constructor(config: AbstractBuilder<BOOSTER, out AbstractBoosterCatalogService<BOOSTER>>) : BoosterCatalog<BOOSTER>, BoosterFetcher {

    @Volatile
    private var boosters = emptySet<BOOSTER>()

    private val catalogProvider: BoosterCatalogProvider

    private val sourceProvider: BoosterCatalogSourceProvider

    private val indexFilter: Predicate<BOOSTER>

    private val listener: ((booster: Booster) -> Any)

    private val transformer: (data: Map<String, Any?>) -> Map<String, Any?>

    private val executor: ExecutorService

    @Volatile
    private var indexResult: CompletableFuture<Set<BOOSTER>>? = null

    @Volatile
    private var prefetchResult: CompletableFuture<Set<BOOSTER>>? = null

    // Return all indexed boosters, except the ones that were marked ignored
    // and the ones that don't pass the global `indexFilter`
    protected val prefilteredBoosters: Stream<BOOSTER>
        get() = boosters.stream().filter(indexFilter).filter(ignored(false))

    init {
        this.catalogProvider = config.catalogProvider ?: config.discoverCatalogProvider()
        this.sourceProvider = config.sourceProvider ?: config.discoverCatalogSourceProvider()
        this.indexFilter = config.filter
        this.listener = config.listener
        this.transformer = config.transformer
        this.executor = config.executor ?: ForkJoinPool.commonPool()
        logger.info("Using " + sourceProvider.javaClass.name)
    }

    /**
     * Indexes the existing YAML files provided by the [BoosterCatalogSourceProvider] implementation.
     * Running this method multiple times has no effect. To cause a re-index call `reindex()`
     */
    fun index(): CompletableFuture<Set<BOOSTER>> {
        return index(false)
    }

    /**
     * Re-runs the indexing of the catalog and the boosters
     * Attention: this won't do anything if indexing is already in progress
     */
    fun reindex(): CompletableFuture<Set<BOOSTER>> {
        return index(true)
    }

    @Synchronized
    private fun index(reindex: Boolean): CompletableFuture<Set<BOOSTER>> {
        var ir = indexResult
        if (!reindex && ir == null || reindex && ir != null && ir.isDone) {
            ir = CompletableFuture.supplyAsync(Supplier<Set<BOOSTER>> {
                try {
                    val bs = ConcurrentSkipListSet(Comparator.comparing<BOOSTER, String>({ it.id }))
                    if (!reindex) {
                        // The first time we immediately set the global set of boosters to be the
                        // newly created empty set. This way users can see the list grow while it's
                        // being populated.
                        boosters = bs
                    }
                    doIndex(bs)
                    if (reindex) {
                        // For re-indexing we set the global list of boosters at the end of the
                        // indexing process. This way users keep seeing the full existing list
                        // until re-indexing has terminated.
                        boosters = bs
                    }
                    bs
                } catch (ex: IOException) {
                    throw UncheckedIOException(ex)
                }
            }, executor)
            indexResult = ir
        }
        return ir!!
    }

    /**
     * Pre-fetches the code for [Booster]s that were found when running [.index].
     * It's not necessary to run this because [Booster] code will be downloaded on
     * demand, but if you want to avoid any delays for the user you can run this method.
     */
    @Synchronized
    fun prefetchBoosters(): CompletableFuture<Set<BOOSTER>> {
        assert(indexResult != null)
        var pr = prefetchResult
        if (pr == null) {
            pr = CompletableFuture.supplyAsync(Supplier<Set<BOOSTER>> {
                logger.info { "Pre-fetching boosters..." }
                for (b in boosters) {
                    try {
                        b.content().get()
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: Exception) {
                        // We ignore errors and go on to fetch the next Booster
                        logger.log(Level.SEVERE, "Error while fetching booster '" + b.name + "'", e)
                    }

                }
                logger.info { "Finished prefetching boosters" }
                boosters
            }, executor)
            prefetchResult = pr
        }
        return pr!!
    }

    /**
     * Clones a Booster repo and provides the path where to find it as a result
     */
    override fun fetchBoosterContent(booster: Booster): CompletableFuture<Path> {
        synchronized(booster) {
            val contentResult = CompletableFuture.supplyAsync(Supplier<Path> {
                sourceProvider(booster)
            }, executor)
            return contentResult
        }
    }

    /**
     * Copies the [Booster] contents to the specified [Path]
     */
    @Throws(IOException::class)
    override fun copy(booster: BOOSTER, projectRoot: Path): Path {
        try {
            val modulePath = booster.content().get()
            return Files.walkFileTree(modulePath,
                    CopyFileVisitor(projectRoot, Predicate { p -> !EXCLUDED_PROJECT_FILES.contains(p.toFile().name.toLowerCase()) }))
        } catch (ex: InterruptedException) {
            throw IOException("Unable to copy Booster", ex)
        } catch (ex: ExecutionException) {
            throw IOException("Unable to copy Booster", ex)
        }

    }

    override fun getBooster(filter: Predicate<BOOSTER>): Optional<BOOSTER> =
            prefilteredBoosters
                .filter(filter)
                .findAny()

    override fun getBoosters(): Set<BOOSTER> = toBoosters(prefilteredBoosters)

    override fun getBoosters(filter: Predicate<BOOSTER>) = toBoosters(prefilteredBoosters.filter(filter))

    private fun toBoosters(bs: Stream<BOOSTER>): Set<BOOSTER> = bs.collect(Collectors.toSet())

    @Throws(IOException::class)
    private fun doIndex(boosters: MutableSet<BOOSTER>) {
        try {
            indexBoosters(boosters)
            postIndex(boosters)
            logger.info { "Finished content indexing" }
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Error while indexing", e)
            throw e
        }

    }

    @Throws(IOException::class)
    protected open fun indexBoosters(boosters: MutableSet<BOOSTER>) {
        val catalog = catalogProvider()
        boosters.addAll(catalog.map { b -> newBooster(b, this) })
    }

    protected open fun postIndex(boosters: MutableSet<BOOSTER>) {
        // Notify the listener of all the boosters that were added
        // (this excludes ignored boosters and those filtered by the global indexFilter)
        prefilteredBoosters.forEach { listener(it) }
    }

    // We take the relative path of the booster name, remove the file extension
    // and turn any path symbols into underscores to create a unique booster id.
    // Eg. "http-crud/vertx/booster.yaml" becomes "http-crud_vertx_booster"
    private fun makeBoosterId(catalogPath: Path, boosterPath: Path): String {
        val relativePath = catalogPath.relativize(boosterPath)
        val pathString = io.fabric8.launcher.booster.Files.removeFileExtension(relativePath.toString().toLowerCase())
        return pathString.replace('/', '_').replace('\\', '_')
    }

    protected abstract fun newBooster(data: Map<String, Any?>, boosterFetcher: BoosterFetcher): BOOSTER

    /**
     * [BoosterCatalogService] Builder class
     *
     * @author [George Gastaldi](mailto:ggastald@redhat.com)
     */
    abstract class AbstractBuilder<BOOSTER : Booster, CATALOG : AbstractBoosterCatalogService<BOOSTER>> {

        var catalogRepositoryURI = LauncherConfiguration.boosterCatalogRepositoryURI()

        var catalogRef = LauncherConfiguration.boosterCatalogRepositoryRef()

        var rootDir: Path? = null

        var catalogProvider: BoosterCatalogProvider? = null

        var sourceProvider: BoosterCatalogSourceProvider? = null

        var filter: Predicate<BOOSTER> = Predicate { true }

        var listener: (booster: Booster) -> Any = {}

        var transformer: (data: Map<String, Any?>) -> Map<String, Any?> = { it }

        var executor: ExecutorService? = null

        open fun catalogRef(catalogRef: String): AbstractBuilder<BOOSTER, CATALOG> {
            this.catalogRef = catalogRef
            return this
        }

        open fun catalogRepository(catalogRepositoryURI: String): AbstractBuilder<BOOSTER, CATALOG> {
            this.catalogRepositoryURI = catalogRepositoryURI
            return this
        }

        open fun catalogProvider(catalogProvider: BoosterCatalogProvider): AbstractBuilder<BOOSTER, CATALOG> {
            this.catalogProvider = catalogProvider
            return this
        }

        open fun sourceProvider(sourceProvider: BoosterCatalogSourceProvider): AbstractBuilder<BOOSTER, CATALOG> {
            this.sourceProvider = sourceProvider
            return this
        }

        open fun filter(filter: Predicate<BOOSTER>): AbstractBuilder<BOOSTER, CATALOG> {
            this.filter = filter
            return this
        }

        open fun listener(listener: (booster: Booster) -> Any): AbstractBuilder<BOOSTER, CATALOG> {
            this.listener = listener
            return this
        }

        open fun transformer(transformer: (data: Map<String, Any?>) -> Map<String, Any?>): AbstractBuilder<BOOSTER, CATALOG> {
            this.transformer = transformer
            return this
        }

        open fun executor(executor: ExecutorService): AbstractBuilder<BOOSTER, CATALOG> {
            this.executor = executor
            return this
        }

        open fun rootDir(root: Path): AbstractBuilder<BOOSTER, CATALOG> {
            this.rootDir = root
            return this
        }

        abstract fun build(): CATALOG

        private val provider by lazy { NativeGitCatalogProvider(catalogRepositoryURI, catalogRef, rootDir) }

        internal fun discoverCatalogProvider(): BoosterCatalogProvider = provider.fetchCatalog

        internal fun discoverCatalogSourceProvider(): BoosterCatalogSourceProvider = provider.fetchSource
    }

    companion object {
        /**
         * Files to be excluded from project creation
         */
        val EXCLUDED_PROJECT_FILES: List<String> = Collections
                .unmodifiableList(Arrays.asList(
                        ".git", ".travis", ".travis.yml",
                        ".ds_store", ".obsidian", ".gitmodules"))

        private const val CLONED_BOOSTERS_DIR = ".boosters"

        private val logger = Logger.getLogger(AbstractBoosterCatalogService::class.java.name)

        fun ignored(ignored: Boolean) = Predicate { b: Booster -> b.isIgnore == ignored }
    }
}
