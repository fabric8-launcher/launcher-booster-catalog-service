package io.fabric8.launcher.booster.catalog.rhoar

import io.fabric8.launcher.booster.catalog.AbstractBoosterCatalogService
import io.fabric8.launcher.booster.catalog.Booster
import io.fabric8.launcher.booster.catalog.BoosterFetcher
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogProvider
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogSourceProvider
import io.fabric8.launcher.booster.catalog.spi.BoosterMetadataProvider
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.collections.HashMap

class RhoarBoosterCatalogService protected constructor(config: Builder) : AbstractBoosterCatalogService<RhoarBooster>(config), RhoarBoosterCatalog {
    var metadataProvider: BoosterMetadataProvider? = null

    init {
        this.metadataProvider = config.metadataProvider ?: config.discoverMetadataProvider()
    }

    override fun newBooster(data: Map<String, Any?>, boosterFetcher: BoosterFetcher) = RhoarBooster(data, boosterFetcher)

    override fun getMissions() = toMissions(prefilteredBoosters)

    override fun getMissions(filter: Predicate<RhoarBooster>) = toMissions(prefilteredBoosters.filter(filter))

    override fun getRuntimes() = toRuntimes(prefilteredBoosters)

    override fun getRuntimes(filter: Predicate<RhoarBooster>) = toRuntimes(prefilteredBoosters.filter(filter))

    override fun getVersions(filter: Predicate<RhoarBooster>) = toVersions(prefilteredBoosters.filter(filter))

    override fun getVersions(mission: Mission, runtime: Runtime) = getVersions(BoosterPredicates.withMission(mission).and(BoosterPredicates.withRuntime(runtime)))

    override fun getBooster(mission: Mission, runtime: Runtime, version: Version?) =
            prefilteredBoosters
                .filter(BoosterPredicates.withMission(mission))
                .filter(BoosterPredicates.withRuntime(runtime))
                .filter(BoosterPredicates.withVersion(version))
                .findAny()

    private fun toRuntimes(bs: Stream<RhoarBooster>) =
            bs
                .filter { b -> b.runtime != null }
                .map<Runtime>({ it.runtime })
                .collect(Collectors.toCollection(Supplier<TreeSet<Runtime>> { TreeSet() }))

    private fun toMissions(bs: Stream<RhoarBooster>) =
            bs
                .filter { b -> b.mission != null }
                .map<Mission>({ it.mission })
                .collect(Collectors.toCollection(Supplier<TreeSet<Mission>> { TreeSet() }))

    private fun toVersions(bs: Stream<RhoarBooster>) =
            bs
                .filter { b -> b.version != null }
                .map<Version>({ it.version })
                .collect(Collectors.toCollection(Supplier<TreeSet<Version>> { TreeSet() }))

    @Throws(IOException::class)
    override fun indexBoosters(boosters: MutableSet<RhoarBooster>) {
        super.indexBoosters(boosters)

        // Update the boosters with the proper info for missions, runtimes and versions
        val missions = HashMap<String, Mission>()
        val runtimes = HashMap<String, Runtime>()

        // Read the metadata for missions and runtimes
        val metadata = metadataProvider?.invoke()
        if (metadata != null) {
            processMetadata(metadata, missions, runtimes)
        }

        for (booster in boosters) {
            var rname = Objects.toString(booster.metadata["runtime"])
            var r: Runtime? = runtimes[rname]
            if (r == null) {
                r = Runtime(rname ?: "<<missing>>")
                logger.log(Level.WARNING, "Runtime ''{0}'' not found in metadata", r.id)
            }
            booster.runtime = r

            var vname = Objects.toString(booster.metadata["version"])
            var v: Version? = r.versions[vname]
            if (v == null) {
                v = Version(vname ?: "<<missing>>")
                logger.log(Level.WARNING, "Version ''{0}'' not found in Runtime ''{1}'' metadata", arrayOf<Any>(v.id, r.id))
            }
            booster.version = v

            var mname = Objects.toString(booster.metadata["mission"])
            var m: Mission? = missions[mname]
            if (m == null) {
                m = Mission(mname ?: "<<missing>>")
                logger.log(Level.WARNING, "Mission ''{0}'' not found in metadata", m.id)
            }
            booster.mission = m
        }
    }

    /**
     * Process the metadataFile and adds to the specified missions and runtimes
     * maps
     */
    fun processMetadata(metadata: Map<String, Any?>, missions: MutableMap<String, Mission>, runtimes: MutableMap<String, Runtime>) {
        try {
            if (metadata["missions"] is List<*>) {
                val ms = metadata["missions"] as List<Map<String, Any?>>
                ms.stream()
                        .map { e ->
                            Mission(
                                    Objects.toString(e["id"]),
                                    Objects.toString(e["name"]),
                                    Objects.toString(e["description"]),
                                    e.getOrDefault("metadata", emptyMap<Any, Any>()) as Map<String, Any?>)
                        }
                        .forEach { m -> missions[m.id] = m }
            }

            if (metadata["runtimes"] is List<*>) {
                val rs = metadata["runtimes"] as List<Map<String, Any?>>
                rs.stream()
                        .map { e ->
                            Runtime(
                                    Objects.toString(e["id"]),
                                    Objects.toString(e["name"]),
                                    Objects.toString(e["description"]),
                                    e.getOrDefault("metadata", emptyMap<String, Any?>()) as Map<String, Any?>,
                                    Objects.toString(e["icon"]),
                                    getMetadataVersions(e["versions"]))
                        }
                        .forEach { r -> runtimes[r.id] = r }
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while processing metadata", e)
        }

    }

    private fun getMetadataVersions(versionsList: Any?): Map<String, Version> {
        if (versionsList is List<*>) {
            val versions = HashMap<String, Version>()
            val vs = versionsList as List<Map<String, Any?>>
            vs.stream()
                    .map { e ->
                        Version(
                                Objects.toString(e["id"]),
                                Objects.toString(e["name"]),
                                Objects.toString(e["description"]),
                                e.getOrDefault("metadata", emptyMap<Any, Any>()) as Map<String, Any?>)
                    }
                    .forEach { v -> versions[v.id] = v }
            return versions
        } else {
            return emptyMap()
        }
    }

    class Builder : AbstractBoosterCatalogService.AbstractBuilder<RhoarBooster, RhoarBoosterCatalogService>() {
        var metadataProvider: BoosterMetadataProvider? = null

        open fun metadataProvider(metadataProvider: BoosterMetadataProvider): Builder {
            this.metadataProvider = metadataProvider
            return this
        }

        override fun catalogRef(catalogRef: String) = super.catalogRef(catalogRef) as Builder
        override fun catalogRepository(catalogRepositoryURI: String) = super.catalogRepository(catalogRepositoryURI) as Builder
        override fun catalogProvider(catalogProvider: BoosterCatalogProvider) = super.catalogProvider(catalogProvider) as Builder
        override fun sourceProvider(sourceProvider: BoosterCatalogSourceProvider) = super.sourceProvider(sourceProvider) as Builder
        override fun filter(filter: Predicate<RhoarBooster>) = super.filter(filter) as Builder
        override fun listener(listener: (booster: Booster) -> Any) = super.listener(listener) as Builder
        override fun transformer(transformer: (data: Map<String, Any?>) -> Map<String, Any?>) = super.transformer(transformer) as Builder
        override fun executor(executor: ExecutorService) = super.executor(executor) as Builder
        override fun rootDir(root: Path) = super.rootDir(root) as Builder
        override fun build() = RhoarBoosterCatalogService(this)

        internal fun discoverMetadataProvider(): BoosterMetadataProvider =
                discoverCatalogProvider() as BoosterMetadataProvider
    }

    companion object {

        private const val METADATA_FILE = "metadata.json"

        private val logger = Logger.getLogger(RhoarBoosterCatalogService::class.java.name)
    }
}
