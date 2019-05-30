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
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogPathProvider
import io.fabric8.launcher.booster.catalog.spi.ZipBoosterCatalogPathProvider
import io.fabric8.launcher.booster.catalog.spi.NativeGitBoosterCatalogPathProvider
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

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

    private val provider: BoosterCatalogPathProvider

    private val indexFilter: Predicate<BOOSTER>

    private val listener: ((booster: Booster) -> Any)

    private val transformer: (data: MutableMap<String, Any?>) -> MutableMap<String, Any?>

    private val environment: String?

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
        this.provider = config.pathProvider ?: config.discoverCatalogProvider()
        this.indexFilter = config.filter
        this.listener = config.listener
        this.transformer = config.transformer
        this.environment = config.environment
        this.executor = config.executor ?: ForkJoinPool.commonPool()
        logger.info("Using " + provider.javaClass.name)
    }

    /**
     * Indexes the existing YAML files provided by the [BoosterCatalogPathProvider] implementation.
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
            var contentResult = CompletableFuture<Path>()
            val contentPath = booster.contentPath
            if (contentPath != null && Files.notExists(contentPath)) {
                contentResult = CompletableFuture.supplyAsync(Supplier<Path> {
                    try {
                        Files.createDirectories(contentPath)
                        provider.createBoosterContentPath(booster)
                        contentPath
                    } catch (ex: Throwable) {
                        io.fabric8.launcher.booster.Files.deleteRecursively(contentPath)
                        throw RuntimeException(ex)
                    }
                }, executor)
            } else {
                contentResult.complete(contentPath)
            }
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
            val catalogPath = provider.createCatalogPath()
            indexBoosters(catalogPath, boosters)
            postIndex(catalogPath, boosters)
            logger.info { "Finished content indexing" }
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Error while indexing", e)
            throw e
        }

    }

    @Throws(IOException::class)
    protected open fun indexBoosters(catalogPath: Path, boosters: MutableSet<BOOSTER>) {
        indexPath(catalogPath, catalogPath, newBooster(null, this), boosters)
    }

    protected open fun postIndex(catalogPath: Path, boosters: MutableSet<BOOSTER>) {
        // Notify the listener of all the boosters that were added
        // (this excludes ignored boosters and those filtered by the global indexFilter)
        prefilteredBoosters.forEach({ listener(it) })
    }

    private fun indexPath(catalogPath: Path, path: Path, commonBooster: BOOSTER, boosters: MutableSet<BOOSTER>) {
        if (Thread.interrupted()) {
            throw RuntimeException("Interrupted")
        }

        // We skip anything starting with "."
        if (path.startsWith(".")) {
            return
        }

        try {
            if (Files.isDirectory(path)) {
                // We check if a file named `common.yaml` exists and if so
                // we merge it's data with the `commonBooster` before passing
                // that on to `indexPath()`
                val common = path.resolve(COMMON_YAML_FILE)
                val activeCommonBooster: BOOSTER = if (Files.isRegularFile(common)) {
                    val localCommonBooster = readBooster(common)
                    if (localCommonBooster != null) {
                        commonBooster.merged(localCommonBooster) as BOOSTER
                    } else {
                        commonBooster
                    }
                } else {
                    commonBooster
                }

                Files.list(path).use { stream ->
                    stream.forEach { subpath -> indexPath(catalogPath, subpath, activeCommonBooster, boosters) }
                }
            } else {
                val fileName = path.fileName.toString().toLowerCase()
                if (fileName.endsWith("booster.yaml") || fileName.endsWith("booster.yml")) {
                    var b = indexBooster(commonBooster, catalogPath, path)
                    if (b != null) {
                        // Check if we should get a specific environment
                        if (environment != null && !environment.isEmpty()) {
                            val envs = environment.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            b = envs.fold(b) { x, env ->
                                x.forEnvironment(env) as BOOSTER
                            }
                        }
                        boosters.add(b)
                    }
                }
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    }

    // We take the relative path of the booster name, remove the file extension
    // and turn any path symbols into underscores to create a unique booster id.
    // Eg. "http-crud/vertx/booster.yaml" becomes "http-crud_vertx_booster"
    private fun makeBoosterId(catalogPath: Path, boosterPath: Path): String {
        val relativePath = catalogPath.relativize(boosterPath)
        val pathString = io.fabric8.launcher.booster.Files.removeFileExtension(relativePath.toString().toLowerCase())
        return pathString.replace('/', '_').replace('\\', '_')
    }

    /**
     * Takes a YAML file from the repository and indexes it
     *
     * @param file A YAML file from the booster-catalog repository
     * @return a [Booster] or null if the booster could not be read
     */
    private fun indexBooster(common: BOOSTER, catalogPath: Path, file: Path): BOOSTER? {
        logger.info { "Indexing $file ..." }
        var booster = readBooster(file)
        if (booster != null) {
            booster = common.merged(booster) as BOOSTER
            val id = makeBoosterId(catalogPath, file)
            booster.id = id
            val moduleRoot = catalogPath.resolve(CLONED_BOOSTERS_DIR)
            val modulePath = moduleRoot.resolve(id)
            booster.contentPath = modulePath
            booster.setDescriptorFromPath(catalogPath.relativize(file))
        }
        return booster
    }

    protected abstract fun newBooster(data: Map<String, Any?>?, boosterFetcher: BoosterFetcher): BOOSTER

    private fun readBooster(file: Path): BOOSTER? {
        val rep = Representer()
        rep.propertyUtils.isSkipMissingProperties = true
        val yaml = Yaml(YamlConstructor(), rep)
        try {
            Files.newBufferedReader(file).use { reader ->
                val boosterData = transformer(yaml.loadAs(reader, Map::class.java) as MutableMap<String, Any?>)
                return newBooster(boosterData, this)
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while reading $file", e)
            return null
        }

    }

    /**
     * [BoosterCatalogService] Builder class
     *
     * @author [George Gastaldi](mailto:ggastald@redhat.com)
     */
    abstract class AbstractBuilder<BOOSTER : Booster, CATALOG : AbstractBoosterCatalogService<BOOSTER>> {

        var catalogRepositoryURI = LauncherConfiguration.boosterCatalogRepositoryURI()

        var catalogRef = LauncherConfiguration.boosterCatalogRepositoryRef()

        var rootDir: Path? = null

        var pathProvider: BoosterCatalogPathProvider? = null

        var filter: Predicate<BOOSTER> = Predicate { true }

        var listener: (booster: Booster) -> Any = {}

        var transformer: (data: MutableMap<String, Any?>) -> MutableMap<String, Any?> = { it }

        var environment: String? = null

        var executor: ExecutorService? = null

        open fun catalogRef(catalogRef: String): AbstractBuilder<BOOSTER, CATALOG> {
            this.catalogRef = catalogRef
            return this
        }

        open fun catalogRepository(catalogRepositoryURI: String): AbstractBuilder<BOOSTER, CATALOG> {
            this.catalogRepositoryURI = catalogRepositoryURI
            return this
        }

        open fun pathProvider(pathProvider: BoosterCatalogPathProvider): AbstractBuilder<BOOSTER, CATALOG> {
            this.pathProvider = pathProvider
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

        open fun transformer(transformer: (data: MutableMap<String, Any?>) -> MutableMap<String, Any?>): AbstractBuilder<BOOSTER, CATALOG> {
            this.transformer = transformer
            return this
        }

        open fun environment(environment: String): AbstractBuilder<BOOSTER, CATALOG> {
            this.environment = environment
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

        internal fun discoverCatalogProvider(): BoosterCatalogPathProvider =
                if (LauncherConfiguration.ignoreLocalZip()) {
                    NativeGitBoosterCatalogPathProvider(catalogRepositoryURI, catalogRef, rootDir)
                } else {
                    val resource = javaClass.classLoader
                            .getResource(String.format("/booster-catalog-%s.zip", catalogRef))
                    if (resource != null) {
                        ZipBoosterCatalogPathProvider(resource)
                    } else {
                        // Resource not found, fallback to original Git resolution
                        NativeGitBoosterCatalogPathProvider(catalogRepositoryURI, catalogRef, rootDir)
                    }
                }
    }

    companion object {
        /**
         * Files to be excluded from project creation
         */
        val EXCLUDED_PROJECT_FILES: List<String> = Collections
                .unmodifiableList(Arrays.asList(".git", ".travis", ".travis.yml",
                        ".ds_store",
                        ".obsidian", ".gitmodules"))

        private const val CLONED_BOOSTERS_DIR = ".boosters"

        private const val COMMON_YAML_FILE = "common.yaml"

        private val logger = Logger.getLogger(AbstractBoosterCatalogService::class.java.name)

        fun ignored(ignored: Boolean) = Predicate { b: Booster -> b.isIgnore == ignored }
    }
}
